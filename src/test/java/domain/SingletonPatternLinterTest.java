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

class SingletonPatternLinterTest {

    @Test
    void detectsSingletonCandidate(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.singleton.AppConfig",
                "package fixtures.singleton;\n"
                        + "public class AppConfig {\n"
                        + "    private static final AppConfig INSTANCE = new AppConfig();\n"
                        + "    private AppConfig() {}\n"
                        + "    public static AppConfig getInstance() {\n"
                        + "        return INSTANCE;\n"
                        + "    }\n"
                        + "}\n"));

        List<Path> classFiles = List.of(tempDir.resolve("fixtures/singleton/AppConfig.class"));

        SingletonPatternLinter linter = new SingletonPatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("fixtures.singleton.AppConfig"));
        assertTrue(result.contains("Total singleton pattern candidates: 1"));
    }

    @Test
    void ignoresClassWithoutPrivateConstructor(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.singleton.NotSingleton",
                "package fixtures.singleton;\n"
                        + "public class NotSingleton {\n"
                        + "    private static final NotSingleton INSTANCE = new NotSingleton();\n"
                        + "    public NotSingleton() {}\n"
                        + "    public static NotSingleton getInstance() {\n"
                        + "        return INSTANCE;\n"
                        + "    }\n"
                        + "}\n"));

        List<Path> classFiles = List.of(tempDir.resolve("fixtures/singleton/NotSingleton.class"));

        SingletonPatternLinter linter = new SingletonPatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("No singleton pattern candidates found."));
        assertFalse(result.contains("fixtures.singleton.NotSingleton"));
    }
}
