package domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import testsupport.JavaClassFixtureCompiler;

class TooManyParametersLinterTest {

    @Test
    void appliesConfiguredThreshold(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.params.SampleService",
                "package fixtures.params;\n"
                        + "public class SampleService {\n"
                        + "    public void borderline(int a, int b, int c) {}\n"
                        + "}\n"));

        List<Path> classFiles = List.of(tempDir.resolve("fixtures/params/SampleService.class"));

        TooManyParametersLinter strictLinter = new TooManyParametersLinter(2);
        String strictResult = strictLinter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));
        assertTrue(strictResult.contains("Total too-many-parameters issues: 1"));

        TooManyParametersLinter relaxedLinter = new TooManyParametersLinter(3);
        String relaxedResult = relaxedLinter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));
        assertTrue(relaxedResult.contains("No too-many-parameters issues found."));
        assertFalse(relaxedResult.contains("SampleService"));
    }
}
