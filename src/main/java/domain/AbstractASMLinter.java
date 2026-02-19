// Jasmeen
package domain;

import datastorage.ASMReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Abstract base class for linters that use ASM to analyze bytecode.
 * Implements the Template Method pattern for loading and iterating classes.
 */
public abstract class AbstractASMLinter implements Linter {

    protected final ASMReader asmReader;

    public AbstractASMLinter(ASMReader asmReader) {
        this.asmReader = asmReader;
    }

    public AbstractASMLinter() {
        this(new ASMReader());
    }

    @Override
    public String lint(List<File> files) {
        StringBuilder result = new StringBuilder();
        int violationCount = 0;

        try {
            List<ClassNode> classes = asmReader.getClasses(files);

            // Hook for pre-processing all classes if needed (e.g. building a graph)
            preProcessClasses(classes);

            for (ClassNode classNode : classes) {
                if (isSkippableClass(classNode)) {
                    continue;
                }

                violationCount += lintClass(classNode, classes, result);
            }

        } catch (IOException e) {
            return "Error reading class files: " + e.getMessage();
        }

        if (violationCount == 0) {
            return getNoViolationsMessage();
        }

        return getViolationsMessage(violationCount, result);
    }

    /**
     * Hook method to process a single class.
     * 
     * @param classNode  The class to analyze
     * @param allClasses List of all classes (context)
     * @param result     StringBuilder to append violations to
     * @return number of violations found in this class
     */
    protected abstract int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result);

    /**
     * Hook method to provide a message when no violations are found.
     */
    protected abstract String getNoViolationsMessage();

    /**
     * Hook method to provide the final report message.
     */
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return result.toString();
    }

    /**
     * Hook method for pre-processing the list of classes. Does nothing by default.
     */
    protected void preProcessClasses(List<ClassNode> classes) {
        // Default implementation does nothing
    }

    /**
     * Helper to skip standard non-user classes.
     * Can be overridden if a linter *wants* to check interfaces/enums.
     */
    protected boolean isSkippableClass(ClassNode classNode) {
        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0)
            return true;
        if ((classNode.access & Opcodes.ACC_ENUM) != 0)
            return true;
        if ((classNode.access & Opcodes.ACC_ANNOTATION) != 0)
            return true;
        return classNode.name.contains("$");
    }
}
