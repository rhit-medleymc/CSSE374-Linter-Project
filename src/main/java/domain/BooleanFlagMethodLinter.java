package domain;

import datastorage.ASMReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BooleanFlagMethodLinter implements Linter {
    private final ASMReader asmReader;

    public BooleanFlagMethodLinter() {
        this.asmReader = new ASMReader();
    }

    public BooleanFlagMethodLinter(ASMReader asmReader) {
        this.asmReader = asmReader;
    }

    @Override
    public String lint(List<File> files) {
        StringBuilder result = new StringBuilder();
        int violationCount = 0;

        try {
            List<ClassNode> classes = asmReader.getClasses(files);
            for (ClassNode classNode : classes) {
                @SuppressWarnings("unchecked")
                List<MethodNode> methods = (List<MethodNode>) classNode.methods;
                for (MethodNode method : methods) {
                    if (isSkippableMethod(method)) {
                        continue;
                    }
                    int booleanParameterCount = countBooleanParameters(Type.getArgumentTypes(method.desc));
                    if (booleanParameterCount > 0) {
                        violationCount++;
                        result.append("Class: ").append(classNode.name.replace('/', '.'))
                                .append(System.lineSeparator());
                        result.append("  Method '").append(method.name)
                                .append("' has ").append(booleanParameterCount)
                                .append(" boolean flag parameter(s).")
                                .append(System.lineSeparator());
                    }
                }
            }
        } catch (IOException e) {
            return "Error reading class files: " + e.getMessage();
        }

        if (violationCount == 0) {
            return "No boolean flag method issues found.";
        }

        result.append("Total boolean flag method issues: ").append(violationCount);
        return result.toString();
    }

    private boolean isSkippableMethod(MethodNode method) {
        if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
            return true;
        }
        if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
            return true;
        }
        return (method.access & Opcodes.ACC_BRIDGE) != 0;
    }

    private int countBooleanParameters(Type[] argumentTypes) {
        int count = 0;
        for (Type argumentType : argumentTypes) {
            if (argumentType.getSort() == Type.BOOLEAN
                    || "Ljava/lang/Boolean;".equals(argumentType.getDescriptor())) {
                count++;
            }
        }
        return count;
    }
}
