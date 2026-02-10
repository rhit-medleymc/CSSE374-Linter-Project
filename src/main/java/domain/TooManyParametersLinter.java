package domain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class TooManyParametersLinter implements Linter {
    private static final int PARAMETER_LIMIT = 5;

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
            return "No too-many-parameters issues found.";
        }

        result.append("Total too-many-parameters issues: ").append(violationCount);
        return result.toString();
    }

    private int lintClassFile(File file, StringBuilder result) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        int count = 0;
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;
        for (MethodNode method : methods) {
            int paramCount = Type.getArgumentTypes(method.desc).length;
            if (paramCount > PARAMETER_LIMIT) {
                if (count == 0) {
                    result.append("File: ").append(file.getPath()).append(System.lineSeparator());
                }
                count++;
                String kind = method.name.equals("<init>") ? "Constructor" : "Method";
                String name = method.name.equals("<init>") ? classNode.name.replace('/', '.') : method.name;
                result.append("  ").append(kind).append(" '").append(name)
                        .append("' has ").append(paramCount)
                        .append(" parameters (limit ").append(PARAMETER_LIMIT).append(")")
                        .append(System.lineSeparator());
            }
        }
        return count;
    }

    private int lintJavaSource(File file, StringBuilder result) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        int count = 0;

        boolean inBlockComment = false;
        StringBuilder signature = new StringBuilder();
        int signatureStartLine = -1;
        boolean inSignature = false;

        for (int i = 0; i < lines.size(); i++) {
            boolean[] inBlockState = new boolean[]{inBlockComment};
            String cleaned = stripComments(lines.get(i), inBlockState);
            inBlockComment = inBlockState[0];

            if (cleaned.trim().isEmpty()) {
                continue;
            }

            if (!inSignature) {
                if (looksLikeMethodOrCtorStart(cleaned)) {
                    inSignature = true;
                    signature.setLength(0);
                    signatureStartLine = i + 1;
                } else {
                    continue;
                }
            }

            signature.append(" ").append(cleaned.trim());
            if (signature.toString().contains(")") && (signature.toString().contains("{")
                    || signature.toString().contains(";"))) {
                String sig = signature.toString();
                int openIdx = sig.indexOf('(');
                int closeIdx = sig.indexOf(')', openIdx + 1);
                if (openIdx >= 0 && closeIdx > openIdx) {
                    String params = sig.substring(openIdx + 1, closeIdx).trim();
                    int paramCount = countParameters(params);
                    if (paramCount > PARAMETER_LIMIT) {
                        if (count == 0) {
                            result.append("File: ").append(file.getPath()).append(System.lineSeparator());
                        }
                        count++;
                        String name = extractMethodOrCtorName(sig);
                        result.append("  Line ").append(signatureStartLine)
                                .append(": '").append(name)
                                .append("' has ").append(paramCount)
                                .append(" parameters (limit ").append(PARAMETER_LIMIT).append(")")
                                .append(System.lineSeparator());
                    }
                }
                inSignature = false;
                signature.setLength(0);
                signatureStartLine = -1;
            }
        }

        return count;
    }

    private int countParameters(String params) {
        if (params.isEmpty()) {
            return 0;
        }
        int count = 0;
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth = Math.max(0, depth - 1);
            } else if (c == ',' && depth == 0) {
                if (!current.toString().trim().isEmpty()) {
                    count++;
                }
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.toString().trim().isEmpty()) {
            count++;
        }
        return count;
    }

    private boolean looksLikeMethodOrCtorStart(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("if ") || trimmed.startsWith("for ")
                || trimmed.startsWith("while ") || trimmed.startsWith("switch ")) {
            return false;
        }
        if (trimmed.contains("=") && !trimmed.contains("(")) {
            return false;
        }
        return trimmed.contains("(");
    }

    private String extractMethodOrCtorName(String signature) {
        int openIdx = signature.indexOf('(');
        if (openIdx < 0) {
            return "unknown";
        }
        String before = signature.substring(0, openIdx).trim();
        String[] tokens = before.split("\\s+");
        if (tokens.length == 0) {
            return "unknown";
        }
        return tokens[tokens.length - 1];
    }

    private String stripComments(String line, boolean[] inBlockComment) {
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
