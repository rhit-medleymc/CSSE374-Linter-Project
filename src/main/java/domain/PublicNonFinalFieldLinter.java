package domain;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import datastorage.ASMReader;

public class PublicNonFinalFieldLinter extends AbstractASMLinter {

    public PublicNonFinalFieldLinter() {
        super();
    }

    public PublicNonFinalFieldLinter(ASMReader asmReader) {
        super(asmReader);
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        int count = 0;
        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;

        for (FieldNode field : fields) {
            boolean isPublic = (field.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isFinal = (field.access & Opcodes.ACC_FINAL) != 0;
            if (isPublic && !isFinal) {
                if (count == 0) {
                    result.append("Class: ").append(classNode.name.replace('/', '.')).append(System.lineSeparator());
                }
                count++;
                result.append("  Public non-final field: ").append(field.name)
                        .append(" (suggest: make it private or final)")
                        .append(System.lineSeparator());
            }
        }
        return count;
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No public non-final field issues found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "Total public non-final field issues: " + violationCount + System.lineSeparator() + result;
    }
}
