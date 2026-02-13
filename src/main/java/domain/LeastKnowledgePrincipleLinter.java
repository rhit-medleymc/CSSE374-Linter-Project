package domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LeastKnowledgePrincipleLinter implements Linter{

    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("\\b(class|interface|enum|record)\\b");

    // Pattern that finds a method call result being accessed (e.g. getA().getB() or getA().field)
    private static final Pattern METHOD_CHAIN_AFTER_CALL = Pattern.compile("\\)\\s*\\.");
    // Pattern that finds chained field/method access like a.b.c or a.b().c
    private static final Pattern MULTI_DOT_CHAIN = Pattern.compile("\\b[A-Za-z_$][\\w$]*\\s*\\.\\s*[A-Za-z_$][\\w$]*\\s*\\.\\s*[A-Za-z_$]");

    @Override
    public String lint(List<File> files) {
        StringBuilder result = new StringBuilder();
        int violationCount = 0;

        for (File file : files) {
            if (!file.exists() || !file.isFile()) {
                result.append("Skipping invalid file: ").append(file.getPath()).append(System.lineSeparator());
                continue;
            }

            if (!file.getName().endsWith(".java")) {
                // Only analyze Java source files for Law of Demeter violations
                continue;
            }

            List<Integer> violatingLines;
            try {
                violatingLines = findDemeterViolations(file);
            } catch (IOException e) {
                result.append("Could not read file: ").append(file.getPath()).append(System.lineSeparator());
                continue;
            }

            if (!violatingLines.isEmpty()) {
                violationCount += violatingLines.size();
                result.append("File: ").append(file.getPath()).append(System.lineSeparator());
                for (Integer ln : violatingLines) {
                    result.append("  Line ").append(ln)
                          .append(": Law of Demeter violation (chained member access or call).")
                          .append(System.lineSeparator());
                }
            }
        }

        if (violationCount == 0) {
            return "No least knowledge principle issues found.";
        }

        result.append("Total least knowledge principle issues: ").append(violationCount);
        return result.toString();
    }

    private List<Integer> findDemeterViolations(File file) throws IOException {
        List<Integer> linesWithIssues = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        StringBuilder statement = new StringBuilder();
        int statementStartLine = -1;
        boolean inBlockComment = false;

        for (int i = 0; i < lines.size(); i++) {
            boolean[] inBlockState = new boolean[]{inBlockComment};
            String cleaned = removeComments(lines.get(i), inBlockState);
            inBlockComment = inBlockState[0];

            if (cleaned.trim().isEmpty()) {
                continue;
            }

            // ignore type declarations and imports/packages - not code statements to check
            if (TYPE_DECLARATION.matcher(cleaned).find() || cleaned.trim().startsWith("import ") || cleaned.trim().startsWith("package ")) {
                statement.setLength(0);
                statementStartLine = -1;
                continue;
            }

            if (statementStartLine < 0) {
                statementStartLine = i + 1;
            }
            statement.append(' ').append(cleaned.trim());

            String current = statement.toString();
            // split on statement terminators so we can analyze full expressions that span lines
            while (true) {
                int semicolon = current.indexOf(';');
                int braceOpen = current.indexOf('{');
                int braceClose = current.indexOf('}');
                int cut = -1;
                if (semicolon >= 0) cut = semicolon;
                else if (braceOpen >= 0) cut = braceOpen;
                else if (braceClose >= 0) cut = braceClose;

                if (cut < 0) {
                    // nothing to split yet; keep accumulating lines
                    statement = new StringBuilder(current);
                    break;
                }

                String oneStatement = current.substring(0, cut).trim();
                if (!oneStatement.isEmpty()) {
                    if (isDemeterViolation(oneStatement)) {
                        linesWithIssues.add(statementStartLine);
                    }
                }

                // move to remainder after the terminator and continue checking
                current = current.substring(cut + 1).trim();
                if (!current.isEmpty()) {
                    statementStartLine = i + 1; // remainder starts on same physical line
                    // continue loop to check the remainder of the line
                } else {
                    statementStartLine = -1;
                    current = "";
                }
                // prepare for next iteration or finish if nothing left
                if (current.isEmpty()) {
                    statement = new StringBuilder();
                    break;
                }
            }
        }

        return linesWithIssues;
    }

    private boolean isDemeterViolation(String statement) {
        // Quick heuristics:
        // - If a method-call result is immediately used to access a member ("\)\.") => violation
        // - If there is a chained access with two or more dots between identifiers (a.b.c) => violation
        // Exclude obvious false-positives like numeric literals or import/package (filtered earlier).

        if (METHOD_CHAIN_AFTER_CALL.matcher(statement).find()) {
            return true;
        }
        if (MULTI_DOT_CHAIN.matcher(statement).find()) {
            return true;
        }
        return false;
    }

    private String removeComments(String line, boolean[] inBlockComment) {
        StringBuilder sb = new StringBuilder();
        boolean inBlock = inBlockComment[0];
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inBlock) {
                if (c == '*' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                    inBlock = false;
                    i++;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                // skip string/char literal content to avoid false positives on '.' inside strings
                char quote = c;
                sb.append(quote);
                i++;
                for (; i < line.length(); i++) {
                    char d = line.charAt(i);
                    sb.append(d);
                    if (d == '\\') { // escape next char
                        if (i + 1 < line.length()) {
                            i++;
                            sb.append(line.charAt(i));
                        }
                        continue;
                    }
                    if (d == quote) {
                        break;
                    }
                }
                continue;
            }
            if (c == '/' && i + 1 < line.length()) {
                char next = line.charAt(i + 1);
                if (next == '/') {
                    break; // rest of line is a comment
                }
                if (next == '*') {
                    inBlock = true;
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        inBlockComment[0] = inBlock;
        return sb.toString();
    }

}
