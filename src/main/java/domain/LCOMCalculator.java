// Jasmeen
package domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Calculates LCOM (Lack of Cohesion of Methods) metrics for Java classes.
 * Uses LCOM4 variant which counts connected components in the method-field
 * dependency graph.
 * 
 * LCOM4 interpretation:
 * - LCOM4 = 1: Highly cohesive class (all methods share fields)
 * - LCOM4 = 2+: Low cohesion, potential SRP violation
 * - Higher values indicate more distinct responsibilities
 */
public class LCOMCalculator {

    /**
     * Represents the LCOM analysis result for a class.
     */
    public static class LCOMResult {
        private final String className;
        private final int lcomScore;
        private final int methodCount;
        private final int fieldCount;
        private final List<Set<String>> components;

        public LCOMResult(String className, int lcomScore, int methodCount, int fieldCount,
                List<Set<String>> components) {
            this.className = className;
            this.lcomScore = lcomScore;
            this.methodCount = methodCount;
            this.fieldCount = fieldCount;
            this.components = components;
        }

        public String getClassName() {
            return className;
        }

        public int getLcomScore() {
            return lcomScore;
        }

        public int getMethodCount() {
            return methodCount;
        }

        public int getFieldCount() {
            return fieldCount;
        }

        public List<Set<String>> getComponents() {
            return components;
        }

        public boolean hasLowCohesion(int threshold) {
            return lcomScore >= threshold;
        }
    }

    /**
     * Calculates LCOM4 for a given class.
     * LCOM4 counts the number of connected components in the method-field
     * dependency graph.
     * 
     * @param classNode the ASM ClassNode to analyze
     * @return LCOMResult containing the analysis
     */
    public LCOMResult calculateLCOM(ClassNode classNode) {
        // Filter out synthetic and static methods/fields for more accurate analysis
        List<MethodNode> instanceMethods = getInstanceMethods(classNode);
        List<FieldNode> instanceFields = getInstanceFields(classNode);

        // Build field access map: method -> set of fields it accesses
        Map<String, Set<String>> methodFieldAccess = buildMethodFieldAccessMap(classNode, instanceMethods,
                instanceFields);

        // Calculate connected components using Union-Find
        List<Set<String>> components = findConnectedComponents(instanceMethods, methodFieldAccess);

        int lcomScore = components.size();

        return new LCOMResult(
                classNode.name,
                lcomScore,
                instanceMethods.size(),
                instanceFields.size(),
                components);
    }

    /**
     * Gets non-synthetic, non-static methods (excluding constructors).
     */
    private List<MethodNode> getInstanceMethods(ClassNode classNode) {
        List<MethodNode> methods = new ArrayList<>();
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            // Skip constructors, static methods, and synthetic methods
            if (!method.name.equals("<init>") &&
                    !method.name.equals("<clinit>") &&
                    (method.access & Opcodes.ACC_STATIC) == 0 &&
                    (method.access & Opcodes.ACC_SYNTHETIC) == 0) {
                methods.add(method);
            }
        }
        return methods;
    }

    /**
     * Gets non-static fields.
     */
    private List<FieldNode> getInstanceFields(ClassNode classNode) {
        List<FieldNode> fields = new ArrayList<>();
        for (FieldNode field : (List<FieldNode>) classNode.fields) {
            if ((field.access & Opcodes.ACC_STATIC) == 0) {
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Builds a map of which fields each method accesses.
     */
    private Map<String, Set<String>> buildMethodFieldAccessMap(ClassNode classNode,
            List<MethodNode> methods,
            List<FieldNode> fields) {
        Map<String, Set<String>> methodFieldAccess = new HashMap<>();
        Set<String> fieldNames = new HashSet<>();

        for (FieldNode field : fields) {
            fieldNames.add(field.name);
        }

        for (MethodNode method : methods) {
            Set<String> accessedFields = new HashSet<>();

            // Analyze method instructions to find field accesses
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    // Check if this field belongs to the current class
                    if (fieldInsn.owner.equals(classNode.name) && fieldNames.contains(fieldInsn.name)) {
                        accessedFields.add(fieldInsn.name);
                    }
                }
            }

            methodFieldAccess.put(method.name, accessedFields);
        }

        return methodFieldAccess;
    }

    /**
     * Finds connected components in the method-field graph using Union-Find
     * algorithm.
     * Two methods are connected if they share at least one field.
     */
    private List<Set<String>> findConnectedComponents(List<MethodNode> methods,
            Map<String, Set<String>> methodFieldAccess) {
        if (methods.isEmpty()) {
            return new ArrayList<>();
        }

        // Union-Find data structure
        Map<String, String> parent = new HashMap<>();

        // Initialize each method as its own parent
        for (MethodNode method : methods) {
            parent.put(method.name, method.name);
        }

        // Find root of a method in Union-Find structure
        class UnionFind {
            String find(String method) {
                if (!parent.get(method).equals(method)) {
                    parent.put(method, find(parent.get(method))); // Path compression
                }
                return parent.get(method);
            }

            void union(String method1, String method2) {
                String root1 = find(method1);
                String root2 = find(method2);
                if (!root1.equals(root2)) {
                    parent.put(root1, root2);
                }
            }
        }

        UnionFind uf = new UnionFind();

        // Group methods that share fields
        Map<String, List<String>> fieldToMethods = new HashMap<>();
        for (MethodNode method : methods) {
            Set<String> fields = methodFieldAccess.get(method.name);
            for (String field : fields) {
                fieldToMethods.computeIfAbsent(field, k -> new ArrayList<>()).add(method.name);
            }
        }

        // Union methods that access the same field
        for (List<String> methodsAccessingField : fieldToMethods.values()) {
            for (int i = 1; i < methodsAccessingField.size(); i++) {
                uf.union(methodsAccessingField.get(0), methodsAccessingField.get(i));
            }
        }

        // Group methods by their root
        Map<String, Set<String>> componentMap = new HashMap<>();
        for (MethodNode method : methods) {
            String root = uf.find(method.name);
            componentMap.computeIfAbsent(root, k -> new HashSet<>()).add(method.name);
        }

        return new ArrayList<>(componentMap.values());
    }
}
