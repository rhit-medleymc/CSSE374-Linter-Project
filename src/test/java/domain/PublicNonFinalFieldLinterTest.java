package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublicNonFinalFieldLinterTest {

    @Test
    void detectsPublicNonFinalFields(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("TestPublicField.java");
        Files.writeString(testFile, String.join(System.lineSeparator(),
                "public class TestPublicField {",
                "    public int badField;",
                "    public static int badStatic;",
                "    public final int okFinal = 1;",
                "    private int okPrivate;",
                "    public static final int OK_CONST = 2;",
                "}"
        ));

        PublicNonFinalFieldLinter linter = new PublicNonFinalFieldLinter();
        String result = linter.lint(List.of(testFile.toFile()));

        assertTrue(result.contains("public non-final field 'badField'"), "Should flag badField");
        assertTrue(result.contains("public non-final field 'badStatic'"), "Should flag badStatic");
        assertTrue(result.contains("Total public non-final field issues: 2"), "Should report total count 2");
    }
}
