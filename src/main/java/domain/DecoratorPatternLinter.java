package domain;

import datastorage.ASMReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Linter that detects the Decorator Pattern in Java bytecode.
 * Extends AbstractASMLinter to reuse ASM class reading logic.
 *
 * Logic:
 * A class is considered a Decorator Candidate if:
 * 1. It extends a class or implements an interface (Target Type).
 * 2. It has a field of that SAME Target Type (Composition).
 */
public class DecoratorPatternLinter extends AbstractASMLinter {

    public DecoratorPatternLinter() {
        super();
    }

    public DecoratorPatternLinter(ASMReader asmReader) {
        super(asmReader);
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        // 1. Identify SuperTypes (Interfaces + Superclass)
        Set<String> superTypes = new HashSet<>();
        if (classNode.superName != null && !"java/lang/Object".equals(classNode.superName)) {
            superTypes.add(classNode.superName);
        }
        if (classNode.interfaces != null) {
            superTypes.addAll((List<String>) classNode.interfaces);
        }

        // 2. Check fields to see if any field type matches a superType
        List<String> decoratedTypes = superTypes.stream()
                .filter(superType -> hasFieldOfType(classNode, superType))
                .collect(Collectors.toList());

        if (!decoratedTypes.isEmpty()) {
            reportDecorator(classNode, decoratedTypes, result);
            return 1;
        }

        return 0;
    }

    private boolean hasFieldOfType(ClassNode classNode, String typeInternalName) {
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            Type fieldType = Type.getType(field.desc);
            if (fieldType.getSort() == Type.OBJECT) {
                if (fieldType.getInternalName().equals(typeInternalName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Option 2 output (student-friendly)
    private void reportDecorator(ClassNode classNode, List<String> decoratedTypes, StringBuilder result) {
        String className = classNode.name.replace('/', '.');

        String types = decoratedTypes.stream()
                .map(t -> t.replace('/', '.'))
                .collect(Collectors.joining(", "));

        result.append(className).append(" looks like a Decorator because:")
                .append(System.lineSeparator());
        result.append("• It extends/implements: ").append(types)
                .append(System.lineSeparator());
        result.append("• It wraps that same type in a field")
                .append(System.lineSeparator());
        result.append(System.lineSeparator());
    }

    // If no decorators are found output
    @Override
    protected String getNoViolationsMessage() {
        return "[Decorator Pattern Results]" + System.lineSeparator()
                + System.lineSeparator()
                + "No Decorator pattern candidates were found."
                + System.lineSeparator();
    }

    // Option 2 header + count
    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "[Decorator Pattern Results]" + System.lineSeparator()
                + System.lineSeparator()
                + violationCount + " Decorator candidate" + (violationCount == 1 ? "" : "s") + " found."
                + System.lineSeparator()
                + System.lineSeparator()
                + result.toString();
    }
}