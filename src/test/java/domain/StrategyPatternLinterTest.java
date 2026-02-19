package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testsupport.JavaClassFixtureCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StrategyPatternLinterTest {

    @Test
    void detectsStrategyCandidateWhenOverseerConstructsMultipleImplementorsAndUsesReturn(@TempDir Path tempDir)
            throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.strategy.Strategy",
                "package fixtures.strategy;\n"
                        + "public interface Strategy { int decide(); }\n",
                "fixtures.strategy.StrategyA",
                "package fixtures.strategy;\n"
                        + "public class StrategyA implements Strategy { public int decide() { return 1; } }\n",
                "fixtures.strategy.StrategyB",
                "package fixtures.strategy;\n"
                        + "public class StrategyB implements Strategy { public int decide() { return 2; } }\n",
                "fixtures.strategy.Manager",
                "package fixtures.strategy;\n"
                        + "import java.util.List;\n"
                        + "public class Manager {\n"
                        + "  private final List<Strategy> strategies = List.of(new StrategyA(), new StrategyB());\n"
                        + "  public int runAll() {\n"
                        + "    int sum = 0;\n"
                        + "    for (Strategy s : strategies) {\n"
                        + "      sum += s.decide();\n"
                        + "    }\n"
                        + "    return sum;\n"
                        + "  }\n"
                        + "}\n"
        ));

        List<Path> classFiles = List.of(
                tempDir.resolve("fixtures/strategy/Strategy.class"),
                tempDir.resolve("fixtures/strategy/StrategyA.class"),
                tempDir.resolve("fixtures/strategy/StrategyB.class"),
                tempDir.resolve("fixtures/strategy/Manager.class")
        );

        StrategyPatternLinter linter = new StrategyPatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("fixtures.strategy.Manager"));
        assertTrue(result.contains("fixtures.strategy.Strategy"));
        assertTrue(result.contains("Total strategy pattern candidates: 1"));
    }

    @Test
    void ignoresWhenInterfaceMethodsAreVoidAndReturnsAreNotUsed(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.strategy2.Strategy",
                "package fixtures.strategy2;\n"
                        + "public interface Strategy { void execute(); }\n",
                "fixtures.strategy2.StrategyA",
                "package fixtures.strategy2;\n"
                        + "public class StrategyA implements Strategy { public void execute() {} }\n",
                "fixtures.strategy2.StrategyB",
                "package fixtures.strategy2;\n"
                        + "public class StrategyB implements Strategy { public void execute() {} }\n",
                "fixtures.strategy2.Manager",
                "package fixtures.strategy2;\n"
                        + "import java.util.List;\n"
                        + "public class Manager {\n"
                        + "  private final java.util.List<Strategy> strategies = java.util.List.of(new StrategyA(), new StrategyB());\n"
                        + "  public void runAll() {\n"
                        + "    for (Strategy s : strategies) {\n"
                        + "      s.execute();\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n"
        ));

        List<Path> classFiles = List.of(
                tempDir.resolve("fixtures/strategy2/Strategy.class"),
                tempDir.resolve("fixtures/strategy2/StrategyA.class"),
                tempDir.resolve("fixtures/strategy2/StrategyB.class"),
                tempDir.resolve("fixtures/strategy2/Manager.class")
        );

        StrategyPatternLinter linter = new StrategyPatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("No strategy pattern candidates found."));
        assertFalse(result.contains("Manager"));
    }
}
