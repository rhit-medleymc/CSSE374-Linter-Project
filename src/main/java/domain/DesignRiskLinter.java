package domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import datastorage.ASMReader;

/**
 * Linter that calculates a "Design Risk Score" for classes.
 * 
 * Scoring Rules:
 * 1. Large Class:
 * - +3 if methods > 15
 * - +2 if fields > 8
 * 2. High Coupling:
 * - +3 if distinct external types referenced > 6
 * 3. Global State Usage:
 * - +2 if class contains static non-final fields
 * 4. Low Cohesion:
 * - +2 if LCOM4 > 1
 * 
 * Risk Interpretation:
 * - 0-3: Low Risk
 * - 4-6: Moderate Risk
 * - 7-10: High Architectural Risk
 */
public class DesignRiskLinter extends AbstractASMLinter {

    private final LCOMCalculator lcomCalculator;

    public DesignRiskLinter(ASMReader asmReader, LCOMCalculator lcomCalculator) {
        super(asmReader);
        this.lcomCalculator = lcomCalculator;
    }

    public DesignRiskLinter() {
        this(new ASMReader(), new LCOMCalculator());
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        int score = 0;
        StringBuilder breakdown = new StringBuilder();

        // 1. Large Class
        int methodCount = classNode.methods.size();
        int fieldCount = classNode.fields.size();
        if (methodCount > 15) {
            score += 3;
            breakdown.append(String.format("  - Large Class (Methods: %d > 15) -> +3\n", methodCount));
        }
        if (fieldCount > 8) {
            score += 2;
            breakdown.append(String.format("  - Large Class (Fields: %d > 8) -> +2\n", fieldCount));
        }

        // 2. High Coupling
        Set<String> dependencies = new HashSet<>();
        // Check fields
        for (FieldNode field : (List<FieldNode>) classNode.fields) {
            addTypeToDependencies(Type.getType(field.desc), dependencies);
        }
        // Check methods (args and return type)
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            addTypeToDependencies(Type.getReturnType(method.desc), dependencies);
            for (Type argType : Type.getArgumentTypes(method.desc)) {
                addTypeToDependencies(argType, dependencies);
            }
        }
        // Remove self and base types
        dependencies.remove(classNode.name);
        dependencies.remove("java/lang/Object");
        dependencies.remove("java/lang/String"); // Often excluded, but user said "external types". Let's keep it strict
                                                 // or loose? User said "distinct external classes". Java core classes
                                                 // are external.
        // Actually, let's keep it simple and just count all object types that are not
        // the class itself.

        // Filter out primitives (already done by Type.getType logic usually handling
        // descriptors, but let's ensure we store internal names)

        if (dependencies.size() > 6) {
            score += 3;
            breakdown.append(String.format("  - High Coupling (%d external classes > 6) -> +3\n", dependencies.size()));
        }

        // 3. Global State
        boolean hasStaticMutable = false;
        for (FieldNode field : (List<FieldNode>) classNode.fields) {
            boolean isStatic = (field.access & Opcodes.ACC_STATIC) != 0;
            boolean isFinal = (field.access & Opcodes.ACC_FINAL) != 0;
            if (isStatic && !isFinal) {
                hasStaticMutable = true;
                break;
            }
        }
        if (hasStaticMutable) {
            score += 2;
            breakdown.append("  - Global State (Static mutable field found) -> +2\n");
        }

        // 4. Low Cohesion
        LCOMCalculator.LCOMResult lcomResult = lcomCalculator.calculateLCOM(classNode);
        if (lcomResult.getLcomScore() > 1) {
            score += 2;
            breakdown
                    .append(String.format("  - Low Cohesion (LCOM4 score: %d > 1) -> +2\n", lcomResult.getLcomScore()));
        }

        if (score > 0) {
            result.append(String.format("Class: %s\n", classNode.name));
            result.append(breakdown);
            result.append(String.format("  Total Score = %d/10 (%s)\n", score, getRiskLevel(score)));
            result.append("\n");
            return 1; // Count as 1 violation (the class itself is the risk)
        }

        return 0;
    }

    private String getRiskLevel(int score) {
        if (score <= 3)
            return "Low Risk";
        if (score <= 6)
            return "Moderate Risk";
        return "High Architectural Risk";
    }

    private void addTypeToDependencies(Type type, Set<String> dependencies) {
        if (type.getSort() == Type.OBJECT) {
            dependencies.add(type.getInternalName());
        } else if (type.getSort() == Type.ARRAY) {
            addTypeToDependencies(type.getElementType(), dependencies);
        }
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No design risks found above threshold.";
    }
}
