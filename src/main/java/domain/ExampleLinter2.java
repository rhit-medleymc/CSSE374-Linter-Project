package domain;

import java.io.File;
import java.util.List;

public class ExampleLinter2 implements Linter {

    @Override
    public String lint(List<File> files) {
        return "ExampleLinter2 is loaded but has no checks yet.";
    }
}
