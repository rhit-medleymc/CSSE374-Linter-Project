package domain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PublicNonFinalFieldLinter implements Linter {

    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("\\b(class|interface|enum|record)\\b");
    private static final Set<String> MODIFIERS = Set.of(
            "public", "protected", "private", "static", "final",
            "volatile", "transient", "abstract", "synchronized", "native", "strictfp"
    );

    @Override
    public String lint(List<File> files) {
        StringBuilder result = new StringBuilder();
        int violationCount = 0;

        for (File file : files) {
            if (!file.exists() || !file.isFile()) {
                result.append("Skipping invalid file: ").append(file.getPath()).append(System.lineSeparator());
                continue;
            }

            try {
                if (file.getName().endsWith(".class")) {
                    violationCount += lintClassFile(file, result);
                } else {
                    violationCount += lintJavaSource(file, result);
                }
            } catch (IOException e) {
                result.append("Could not read file: ").append(file.getPath()).append(System.lineSeparator());
            }
        }

        if (violationCount == 0) {
            return "No public non-final field issues found.";
        }

        result.append("Total public non-final field issues: ").append(violationCount);
        return result.toString();
    }

    private int lintClassFile(File file, StringBuilder result) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        int count = 0;
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            boolean isPublic = (field.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isFinal = (field.access & Opcodes.ACC_FINAL) != 0;
            if (isPublic && !isFinal) {
                if (count == 0) {
                    result.append("File: ").append(file.getPath()).append(System.lineSeparator());
                }
                count++;
                result.append("  Public non-final field: ").append(field.name)
                        .append(" (suggest: make it private or final)")
                        .append(System.lineSeparator());
            }
        }
        return count;
    }

    private int lintJavaSource(File file, StringBuilder result) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        int count = 0;

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

            if (TYPE_DECLARATION.matcher(cleaned).find()) {
                statement.setLength(0);
                statementStartLine = -1;
                continue;
            }

            if (statementStartLine < 0) {
                statementStartLine = i + 1;
            }
            statement.append(" ").append(cleaned.trim());

            String current = statement.toString();
            while (current.contains(";")) {
                int idx = current.indexOf(';');
                String oneStatement = current.substring(0, idx).trim();
                if (!oneStatement.isEmpty()) {
                    if (isPublicNonFinalFieldDeclaration(oneStatement)) {
                        List<String> fieldNames = extractName(oneStatement);
                        if (count == 0) {
                            result.append("File: ").append(file.getPath()).append(System.lineSeparator());
                        }
                        if (fieldNames.isEmpty()) {
                            count++;
                            result.append("  Line ").append(statementStartLine)
                                    .append(": public non-final field detected (suggest: make it private or final)")
                                    .append(System.lineSeparator());
                        } else {
                            for (String fieldName : fieldNames) {
                                count++;
                                result.append("  Line ").append(statementStartLine)
                                        .append(": public non-final field '").append(fieldName)
                                        .append("' detected (suggest: make it private or final)")
                                        .append(System.lineSeparator());
                            }
                        }
                    }
                }
                current = current.substring(idx + 1).trim();
                statement = new StringBuilder();
                if (!current.isEmpty()) {
                    statement.append(current);
                    statementStartLine = i + 1;
                } else {
                    statementStartLine = -1;
                }
            }
        }
        return count;
    }

    private List<String> extractName(String statement) {
        String normalized = statement.replaceAll("\\s+", " ").trim();

        while (normalized.startsWith("@")) {
            int spaceIdx = normalized.indexOf(' ');
            if (spaceIdx < 0) {
                return List.of();
            }
            normalized = normalized.substring(spaceIdx + 1).trim();
        }

        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        int index = 0;
        while (index < tokens.size() && MODIFIERS.contains(tokens.get(index))) {
            index++;
        }
        if (index >= tokens.size() - 1) {
            return List.of();
        }

        StringBuilder rest = new StringBuilder();
        for (int i = index + 1; i < tokens.size(); i++) {
            if (rest.length() > 0) {
                rest.append(' ');
            }
            rest.append(tokens.get(i));
        }

        List<String> names = new ArrayList<>();
        for (String part : rest.toString().split(",")) {
            String candidate = part.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            int eqIdx = candidate.indexOf('=');
            if (eqIdx >= 0) {
                candidate = candidate.substring(0, eqIdx).trim();
            }
            if (candidate.isEmpty()) {
                continue;
            }
            String name = candidate.split("\\s+")[0].trim();
            name = name.replaceAll("\\[\\]$", "");
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private boolean isPublicNonFinalFieldDeclaration(String statement) {
        if (!statement.contains("public")) {
            return false;
        }
        if (statement.contains("final")) {
            return false;
        }
        if (TYPE_DECLARATION.matcher(statement).find()) {
            return false;
        }
        // Heuristic: field declarations must have an identifier and end without braces.
        if (statement.contains("{") || statement.contains("}")) {
            return false;
        }
        return statement.matches(".*\\bpublic\\b.*\\b[A-Za-z_$][\\w$]*\\b.*");
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
            if (c == '/' && i + 1 < line.length()) {
                char next = line.charAt(i + 1);
                if (next == '/') {
                    break;
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
