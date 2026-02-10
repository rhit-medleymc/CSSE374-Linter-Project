package domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TrailingWhitespaceLinter implements Linter {

    @Override
    public String lint(List<File> files) {
        StringBuilder result = new StringBuilder();
        int violationCount = 0;

        for (File file : files) {
            if (!file.exists() || !file.isFile()) {
                result.append("Skipping invalid file: ").append(file.getPath()).append(System.lineSeparator());
                continue;
            }

            List<Integer> linesWithTrailingWhitespace;
            try {
                linesWithTrailingWhitespace = findTrailingWhitespaceLines(file);
            } catch (IOException e) {
                result.append("Could not read file: ").append(file.getPath()).append(System.lineSeparator());
                continue;
            }
            if (!linesWithTrailingWhitespace.isEmpty()) {
                violationCount += linesWithTrailingWhitespace.size();
                result.append("File: ").append(file.getPath()).append(System.lineSeparator());
                result.append("Trailing whitespace found on line(s): ");
                for (int i = 0; i < linesWithTrailingWhitespace.size(); i++) {
                    result.append(linesWithTrailingWhitespace.get(i));
                    if (i < linesWithTrailingWhitespace.size() - 1) {
                        result.append(", ");
                    }
                }
                result.append(System.lineSeparator());
            }
        }

        if (violationCount == 0) {
            return "No trailing whitespace issues found.";
        }

        result.append("Total trailing whitespace issues: ").append(violationCount);
        return result.toString();
    }

    private List<Integer> findTrailingWhitespaceLines(File file) throws IOException {
        List<Integer> linesWithIssues = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        for (int i = 0; i < lines.size(); i++) {
            if (hasTrailingWhitespace(lines.get(i))) {
                linesWithIssues.add(i + 1);
            }
        }
        return linesWithIssues;
    }

    private boolean hasTrailingWhitespace(String line) {
        if (line.isEmpty()) {
            return false;
        }
        char last = line.charAt(line.length() - 1);
        return last == ' ' || last == '\t';
    }
}
