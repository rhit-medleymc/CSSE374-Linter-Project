package datastorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
            files.add(this.file);
        }
        return files;
    }

    public File getFile() {
        return file;
    }
}
