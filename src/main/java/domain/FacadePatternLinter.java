package domain;

import datastorage.ASMReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FacadePatternLinter extends AbstractASMLinter {

    private Set<String> internalClassNames;

    public FacadePatternLinter() {
        super();
    }

    public FacadePatternLinter(ASMReader asmReader) {
        super(asmReader);
    }

    @Override
    protected void preProcessClasses(List<ClassNode> classes) {
        internalClassNames = classes.stream()
                .map(classNode -> classNode.name)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        FacadeCandidate candidate = analyzeCandidate(classNode, internalClassNames);
        if (candidate.isFacade) {
            appendCandidate(result, candidate);
            return 1;
        }
        return 0;
    }

    private FacadeCandidate analyzeCandidate(ClassNode classNode, Set<String> internalClassNames) {
        Set<String> fieldSubsystems = collectFieldSubsystems(classNode, internalClassNames);
        Set<String> invokedSubsystems = new TreeSet<>();

        int publicMethodCount = 0;
        int delegatingPublicMethodCount = 0;

        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;
        for (MethodNode method : methods) {
            if (!isPublicBusinessMethod(method)) {
                continue;
            }
            publicMethodCount++;

            Set<String> invokedInMethod = collectInvokedInternalTypes(method, classNode.name, internalClassNames);
            if (!invokedInMethod.isEmpty()) {
                delegatingPublicMethodCount++;
                invokedSubsystems.addAll(invokedInMethod);
            }
        }

        boolean enoughSubsystems = invokedSubsystems.size() >= 2;
        boolean mostlyDelegating = publicMethodCount > 0
                && delegatingPublicMethodCount >= Math.max(1, publicMethodCount / 2);
        boolean hasSubsystemState = !fieldSubsystems.isEmpty();

        boolean isFacade = enoughSubsystems && mostlyDelegating && (hasSubsystemState || publicMethodCount <= 5);

        return new FacadeCandidate(
                classNode.name,
                publicMethodCount,
                delegatingPublicMethodCount,
                fieldSubsystems,
                invokedSubsystems,
                isFacade);
    }

    private Set<String> collectFieldSubsystems(ClassNode classNode, Set<String> internalClassNames) {
        Set<String> subsystemTypes = new TreeSet<>();
        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            for (String internalType : extractObjectTypes(field.desc)) {
                if (internalClassNames.contains(internalType) && !internalType.equals(classNode.name)) {
                    subsystemTypes.add(internalType);
                }
            }
        }
        return subsystemTypes;
    }

    private Set<String> collectInvokedInternalTypes(MethodNode method, String ownerName,
            Set<String> internalClassNames) {
        Set<String> owners = new TreeSet<>();
        for (AbstractInsnNode instruction = method.instructions
                .getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
                if (!methodInstruction.owner.equals(ownerName)
                        && internalClassNames.contains(methodInstruction.owner)) {
                    owners.add(methodInstruction.owner);
                }
            }
        }
        return owners;
    }

    private List<String> extractObjectTypes(String descriptor) {
        List<String> types = new ArrayList<>();
        Type type = Type.getType(descriptor);
        extractObjectTypes(type, types);
        return types;
    }

    private void extractObjectTypes(Type type, List<String> types) {
        if (type.getSort() == Type.OBJECT) {
            types.add(type.getInternalName());
            return;
        }
        if (type.getSort() == Type.ARRAY) {
            extractObjectTypes(type.getElementType(), types);
        }
    }

    private boolean isPublicBusinessMethod(MethodNode method) {
        if ((method.access & Opcodes.ACC_PUBLIC) == 0) {
            return false;
        }
        if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
            return false;
        }
        if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
            return false;
        }
        return (method.access & Opcodes.ACC_BRIDGE) == 0;
    }

    private void appendCandidate(StringBuilder result, FacadeCandidate candidate) {
        result.append("Class: ").append(candidate.className.replace('/', '.')).append(System.lineSeparator());
        result.append("  Delegating public methods: ")
                .append(candidate.delegatingPublicMethodCount)
                .append(" / ")
                .append(candidate.publicMethodCount)
                .append(System.lineSeparator());
        result.append("  Invoked subsystem classes: ")
                .append(formatTypeSet(candidate.invokedSubsystems))
                .append(System.lineSeparator());
        if (!candidate.fieldSubsystems.isEmpty()) {
            result.append("  Field subsystem classes: ")
                    .append(formatTypeSet(candidate.fieldSubsystems))
                    .append(System.lineSeparator());
        }
    }

    private String formatTypeSet(Set<String> typeNames) {
        return typeNames.stream()
                .map(type -> type.replace('/', '.'))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No facade pattern candidates found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "Total facade pattern candidates: " + violationCount + System.lineSeparator() + result.toString();
    }

    private static class FacadeCandidate {
        private final String className;
        private final int publicMethodCount;
        private final int delegatingPublicMethodCount;
        private final Set<String> fieldSubsystems;
        private final Set<String> invokedSubsystems;
        private final boolean isFacade;

        private FacadeCandidate(
                String className,
                int publicMethodCount,
                int delegatingPublicMethodCount,
                Set<String> fieldSubsystems,
                Set<String> invokedSubsystems,
                boolean isFacade) {
            this.className = className;
            this.publicMethodCount = publicMethodCount;
            this.delegatingPublicMethodCount = delegatingPublicMethodCount;
            this.fieldSubsystems = fieldSubsystems;
            this.invokedSubsystems = invokedSubsystems;
            this.isFacade = isFacade;
        }
    }
}
