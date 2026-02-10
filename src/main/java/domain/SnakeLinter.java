package domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Linter that checks if class names, field names, and variable names follow snake_case convention.
 * Reports any violations with line numbers and the violating identifier.
 */
public class SnakeLinter implements Linter {

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(?:public|private|protected)?\\s*(?:abstract)?\\s*class\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\b(?:public|private|protected|static|final)+\\s+(?:int|String|boolean|double|float|long|char|byte|short|void|[a-zA-Z_][a-zA-Z0-9_]*)(?:\\[\\])*\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=;]");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\b(?:int|String|boolean|double|float|long|char|byte|short|void|[a-zA-Z_][a-zA-Z0-9_]*)(?:\\[\\])*\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=;,)]");
    
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$|^[a-z]$");

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
     * Processes a single Java file and checks for snake_case violations
     */
    private void processJavaFile(File file, StringBuilder violations) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            String[] lines = content.split("\n");
            
            // Check class names
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                // Check class declarations
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
                while (classMatcher.find()) {
                    String className = classMatcher.group(1);
                    if (isSnakeCaseViolation(className)) {
                        violations.append(file.getName()).append(":").append(i + 1)
                                .append(" - Class name '").append(className).append("' does not follow snake_case\n");
                    }
                }
                
                // Skip comments and string literals for field/variable checking
                String cleanLine = removeComments(line);
                
                // Check field declarations
                Matcher fieldMatcher = FIELD_PATTERN.matcher(cleanLine);
                while (fieldMatcher.find()) {
                    String fieldName = fieldMatcher.group(1);
                    if (isSnakeCaseViolation(fieldName)) {
                        violations.append(file.getName()).append(":").append(i + 1)
                                .append(" - Field name '").append(fieldName).append("' does not follow snake_case\n");
                    }
                }
                
                // Check variable declarations
                Matcher variableMatcher = VARIABLE_PATTERN.matcher(cleanLine);
                while (variableMatcher.find()) {
                    String variableName = variableMatcher.group(1);
                    if (isSnakeCaseViolation(variableName) && !isKeywordOrType(variableName)) {
                        violations.append(file.getName()).append(":").append(i + 1)
                                .append(" - Variable name '").append(variableName).append("' does not follow snake_case\n");
                    }
                }
            }
        } catch (IOException e) {
            // Skip files that cannot be read
        }
    }

    /**
     * Removes single-line comments from a line of code
     */
    private String removeComments(String line) {
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            return line.substring(0, commentIndex);
        }
        return line;
    }

    /**
     * Checks if an identifier violates snake_case convention
     * Returns true if it doesn't follow snake_case
     */
    private boolean isSnakeCaseViolation(String identifier) {
        return !SNAKE_CASE_PATTERN.matcher(identifier).matches();
    }

    /**
     * Checks if a string is a Java keyword or primitive type
     */
    private boolean isKeywordOrType(String word) {
        String[] keywords = {
            "int", "String", "boolean", "double", "float", "long", "char", "byte", "short", "void",
            "public", "private", "protected", "static", "final", "class", "interface", "extends",
            "implements", "new", "return", "if", "else", "for", "while", "do", "switch", "case",
            "default", "break", "continue", "try", "catch", "finally", "throw", "throws", "import",
            "package", "abstract", "synchronized", "volatile", "transient", "native", "strictfp",
            "super", "this", "true", "false", "null"
        };
        
        for (String keyword : keywords) {
            if (word.equals(keyword)) {
                return true;
            }
        }
        return false;
    }
}