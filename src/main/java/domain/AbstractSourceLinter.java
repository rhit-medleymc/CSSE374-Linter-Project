//Jasmeen
package domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Abstract base class for linters that analyze source code text directly.
 * Implements the Template Method pattern for file traversal and content
 * reading.
 */
public abstract class AbstractSourceLinter implements Linter {

    @Override
    public String lint(List<File> files) {
        StringBuilder violations = new StringBuilder();

        for (File file : files) {
            if (file.isDirectory()) {
                processDirectory(file, violations);
            } else if (file.getName().endsWith(".java")) {
                processJavaFile(file, violations);
            }
        }

        return violations.length() == 0 ? "" : violations.toString();
    }

    /**
     * Recursively processes all Java files in a directory
     */
    private void processDirectory(File dir, StringBuilder violations) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(file, violations);
                } else if (file.getName().endsWith(".java")) {
                    processJavaFile(file, violations);
                }
            }
        }
    }

    /**
     * Reads a Java file and delegates linting to the concrete implementation.
     */
    private void processJavaFile(File file, StringBuilder violations) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            lintFile(file, content, violations);
        } catch (IOException e) {
            // Skip files that cannot be read
            System.err.println("Error reading file: " + file.getAbsolutePath());
        }
    }

    /**
     * Concrete implementations must provide logic to lint a single file's content.
     * 
     * @param file       The file being analyzed (for name/path access)
     * @param content    The full text content of the file
     * @param violations StringBuilder to specific violations to
     */
    protected abstract void lintFile(File file, String content, StringBuilder violations);

    /**
     * Helper to remove single-line comments from a line of code.
     * Useful for many source-based linters.
     */
    protected String removeComments(String line) {
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            return line.substring(0, commentIndex);
        }
        return line;
    }
}
