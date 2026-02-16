package domain;

import datastorage.ASMReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AdapterPatternLinter extends AbstractASMLinter {

    public AdapterPatternLinter() {
        super();
    }

    public AdapterPatternLinter(ASMReader asmReader) {
        super(asmReader);
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        AdapterCandidate candidate = analyzeClass(classNode);
        if (candidate.isAdapter) {
            appendCandidate(result, candidate);
            return 1;
        }
        return 0;
    }

    private AdapterCandidate analyzeClass(ClassNode classNode) {
        Set<String> targetTypes = collectTargetTypes(classNode);
        Set<String> adapteeFieldTypes = collectAdapteeFieldTypes(classNode);
        Set<String> delegatedOwners = new TreeSet<>();

        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;
        for (MethodNode method : methods) {
            if (isSkippableMethod(method)) {
                continue;
            }
            delegatedOwners.addAll(collectDelegatedOwners(method, adapteeFieldTypes));
        }

        boolean hasTargetType = !targetTypes.isEmpty();
        boolean hasAdapteeState = !adapteeFieldTypes.isEmpty();
        boolean delegatesToAdaptee = !delegatedOwners.isEmpty();

        boolean isAdapter = hasTargetType && hasAdapteeState && delegatesToAdaptee;
        return new AdapterCandidate(classNode.name, targetTypes, adapteeFieldTypes, delegatedOwners, isAdapter);
    }

    private Set<String> collectTargetTypes(ClassNode classNode) {
        Set<String> targetTypes = new TreeSet<>();

        @SuppressWarnings("unchecked")
        List<String> interfaces = (List<String>) classNode.interfaces;
        targetTypes.addAll(interfaces);

        if (classNode.superName != null && !"java/lang/Object".equals(classNode.superName)) {
            targetTypes.add(classNode.superName);
        }

        return targetTypes;
    }

    private Set<String> collectAdapteeFieldTypes(ClassNode classNode) {
        Set<String> adapteeTypes = new TreeSet<>();
        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            Type type = Type.getType(field.desc);
            if (type.getSort() == Type.OBJECT) {
                String name = type.getInternalName();
                if (!name.equals(classNode.name) && !name.startsWith("java/")) {
                    adapteeTypes.add(name);
                }
            } else if (type.getSort() == Type.ARRAY && type.getElementType().getSort() == Type.OBJECT) {
                String name = type.getElementType().getInternalName();
                if (!name.equals(classNode.name) && !name.startsWith("java/")) {
                    adapteeTypes.add(name);
                }
            }
        }
        return adapteeTypes;
    }

    private Set<String> collectDelegatedOwners(MethodNode method, Set<String> adapteeFieldTypes) {
        Set<String> delegatedOwners = new TreeSet<>();
        for (AbstractInsnNode instruction = method.instructions
                .getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
                if (adapteeFieldTypes.contains(methodInstruction.owner)) {
                    delegatedOwners.add(methodInstruction.owner);
                }
            }
        }
        return delegatedOwners;
    }

    private boolean isSkippableMethod(MethodNode method) {
        if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
            return true;
        }
        if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
            return true;
        }
        if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
            return true;
        }
        return (method.access & Opcodes.ACC_BRIDGE) != 0;
    }

    private void appendCandidate(StringBuilder result, AdapterCandidate candidate) {
        result.append("Class: ").append(candidate.className.replace('/', '.')).append(System.lineSeparator());
        result.append("  Target type(s): ").append(formatTypes(candidate.targetTypes))
                .append(System.lineSeparator());
        result.append("  Adaptee field type(s): ").append(formatTypes(candidate.adapteeFieldTypes))
                .append(System.lineSeparator());
        result.append("  Delegated adaptee call(s): ").append(formatTypes(candidate.delegatedOwners))
                .append(System.lineSeparator());
    }

    private String formatTypes(Set<String> types) {
        return types.stream()
                .map(type -> type.replace('/', '.'))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No adapter pattern candidates found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "Total adapter pattern candidates: " + violationCount + System.lineSeparator() + result.toString();
    }

    private static class AdapterCandidate {
        private final String className;
        private final Set<String> targetTypes;
        private final Set<String> adapteeFieldTypes;
        private final Set<String> delegatedOwners;
        private final boolean isAdapter;

        private AdapterCandidate(
                String className,
                Set<String> targetTypes,
                Set<String> adapteeFieldTypes,
                Set<String> delegatedOwners,
                boolean isAdapter) {
            this.className = className;
            this.targetTypes = targetTypes;
            this.adapteeFieldTypes = adapteeFieldTypes;
            this.delegatedOwners = delegatedOwners;
            this.isAdapter = isAdapter;
        }
    }
}
