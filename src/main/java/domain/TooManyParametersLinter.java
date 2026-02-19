package domain;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import datastorage.ASMReader;

public class TooManyParametersLinter extends AbstractASMLinter {
    private static final int DEFAULT_PARAMETER_LIMIT = 5;

    private final int parameterLimit;

    public TooManyParametersLinter() {
        super();
        this.parameterLimit = DEFAULT_PARAMETER_LIMIT;
    }

    public TooManyParametersLinter(int parameterLimit) {
        super();
        this.parameterLimit = parameterLimit;
    }

    public TooManyParametersLinter(int parameterLimit, ASMReader asmReader) {
        super(asmReader);
        this.parameterLimit = parameterLimit;
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        int count = 0;
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;

        for (MethodNode method : methods) {
            if (isSkippableMethod(method)) {
                continue;
            }

            int paramCount = Type.getArgumentTypes(method.desc).length;
            if (paramCount > parameterLimit) {
                if (count == 0) {
                    result.append("Class: ").append(classNode.name.replace('/', '.')).append(System.lineSeparator());
                }
                count++;
                String kind = method.name.equals("<init>") ? "Constructor" : "Method";
                String name = method.name.equals("<init>") ? classNode.name.replace('/', '.') : method.name;
                result.append("  ").append(kind).append(" '").append(name)
                        .append("' has ").append(paramCount)
                        .append(" parameters (limit ").append(parameterLimit).append(")")
                        .append(System.lineSeparator());
            }
        }

        return count;
    }

    private boolean isSkippableMethod(MethodNode method) {
        if (method.name.equals("<clinit>")) {
            return true;
        }
        if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
            return true;
        }
        return (method.access & Opcodes.ACC_BRIDGE) != 0;
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No too-many-parameters issues found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "Total too-many-parameters issues: " + violationCount + System.lineSeparator() + result;
    }
}
