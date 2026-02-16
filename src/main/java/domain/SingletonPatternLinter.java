package domain;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import datastorage.ASMReader;

public class SingletonPatternLinter extends AbstractASMLinter {

    public SingletonPatternLinter() {
        super();
    }

    public SingletonPatternLinter(ASMReader asmReader) {
        super(asmReader);
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        if (!hasPrivateConstructors(classNode)) {
            return 0;
        }

        FieldNode singletonField = findSingletonField(classNode);
        if (singletonField == null) {
            return 0;
        }

        if (!hasPublicStaticAccessor(classNode, singletonField)) {
            return 0;
        }

        result.append("Class: ").append(classNode.name.replace('/', '.')).append(System.lineSeparator());
        result.append("  Singleton field: ").append(singletonField.name)
                .append(" (global state candidate)")
                .append(System.lineSeparator());
        return 1;
    }

    private boolean hasPrivateConstructors(ClassNode classNode) {
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;

        int constructorCount = 0;
        for (MethodNode method : methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            constructorCount++;
            if ((method.access & Opcodes.ACC_PRIVATE) == 0) {
                return false;
            }
        }

        return constructorCount > 0;
    }

    private FieldNode findSingletonField(ClassNode classNode) {
        String classDescriptor = Type.getObjectType(classNode.name).getDescriptor();

        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            boolean isStatic = (field.access & Opcodes.ACC_STATIC) != 0;
            if (isStatic && classDescriptor.equals(field.desc)) {
                return field;
            }
        }

        return null;
    }

    private boolean hasPublicStaticAccessor(ClassNode classNode, FieldNode singletonField) {
        String classDescriptor = Type.getObjectType(classNode.name).getDescriptor();

        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;
        for (MethodNode method : methods) {
            if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) {
                continue;
            }

            boolean isPublic = (method.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            boolean returnsSelf = classDescriptor.equals(Type.getReturnType(method.desc).getDescriptor());
            if (!isPublic || !isStatic || !returnsSelf) {
                continue;
            }

            if (loadsSingletonField(classNode, singletonField, method)) {
                return true;
            }
        }

        return false;
    }

    private boolean loadsSingletonField(ClassNode classNode, FieldNode singletonField, MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction
                .getNext()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode fieldInstruction = (FieldInsnNode) instruction;
                if (fieldInstruction.getOpcode() == Opcodes.GETSTATIC
                        && classNode.name.equals(fieldInstruction.owner)
                        && singletonField.name.equals(fieldInstruction.name)
                        && singletonField.desc.equals(fieldInstruction.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No singleton pattern candidates found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "Total singleton pattern candidates: " + violationCount + System.lineSeparator() + result;
    }
}
