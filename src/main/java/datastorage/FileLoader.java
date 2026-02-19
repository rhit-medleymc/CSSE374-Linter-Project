package datastorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FileLoader {
    private File file;

    public List<File> loadFiles(String input) {
        List<File> files = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return files;
        }

        String[] paths = input.split(",");
        for (String path : paths) {
            String trimmed = path.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            this.file = new File(trimmed);
            if (this.file.exists()) {
                files.add(this.file);
            } else {
                System.out.println("Skipping missing path: " + this.file.getPath());
            }
        }

        return expandInputFiles(files);
    }

    public File getFile() {
        return file;
    }

    private List<File> expandInputFiles(List<File> requestedPaths) {
        List<File> files = new ArrayList<>();
        Set<String> seenPaths = new LinkedHashSet<>();

        for (File path : requestedPaths) {
            collectFiles(path, files, seenPaths);
        }

        return files;
    }

    private void collectFiles(File path, List<File> files, Set<String> seenPaths) {
        if (path.isFile()) {
            String absolutePath = path.getAbsolutePath();
            if (seenPaths.add(absolutePath)) {
                files.add(path);
            }
            return;
        }

        if (!path.isDirectory()) {
            return;
        }

        File[] children = path.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        for (File child : children) {
            collectFiles(child, files, seenPaths);
        }
    }
}
