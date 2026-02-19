// Jasmeen
package domain;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import java.util.regex.Pattern;

/**
 * Linter that checks for unused imports in Java source files.
 * Extends AbstractSourceLinter to reuse file processing logic (Template Method
 * Pattern).
 */
public class UnusedImportLinter extends AbstractSourceLinter {

    @Override
    protected void lintFile(File file, String content, StringBuilder violations) {
        String[] lines = content.split("\n");
        Set<ImportInfo> imports = new HashSet<>();

        // Pass 1: Collect all imports
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("import ")) {
                // Check if it's a static import or wildcard import
                if (!line.contains(" static ") && !line.contains("*")) {
                    // Extract full path: import java.util.List; -> java.util.List
                    int semiColonIndex = line.lastIndexOf(';');
                    if (semiColonIndex != -1) {
                        String importPath = line.substring(7, semiColonIndex).trim();
                        // Extract simple name: java.util.List -> List
                        String simpleName = importPath.substring(importPath.lastIndexOf('.') + 1);
                        imports.add(new ImportInfo(simpleName, importPath, i + 1));
                    }
                }
            }
        }

        // Pass 2: Check usage
        for (ImportInfo importInfo : imports) {
            boolean isUsed = false;
            // Simple regex to find usage: word boundary + simpleName + word boundary
            Pattern usagePattern = Pattern.compile("\\b" + Pattern.quote(importInfo.simpleName) + "\\b");

            for (int i = 0; i < lines.length; i++) {
                // Skip the import definition line itself
                if (i + 1 == importInfo.lineNumber)
                    continue;

                String line = removeComments(lines[i]);
                if (usagePattern.matcher(line).find()) {
                    isUsed = true;
                    break;
                }
            }

            if (!isUsed) {
                violations.append(file.getName()).append(":").append(importInfo.lineNumber)
                        .append(" - Unused import: ").append(importInfo.fullPath).append("\n");
            }
        }
    }

    private static class ImportInfo {
        String simpleName;
        String fullPath;
        int lineNumber;

        ImportInfo(String simpleName, String fullPath, int lineNumber) {
            this.simpleName = simpleName;
            this.fullPath = fullPath;
            this.lineNumber = lineNumber;
        }
    }
}
