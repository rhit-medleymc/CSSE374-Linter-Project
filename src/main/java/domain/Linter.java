package domain;

import java.io.File;
import java.util.List;

public interface Linter {
    String lint(List<File> files);
}
