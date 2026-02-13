package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testsupport.JavaClassFixtureCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterPatternLinterTest {

    @Test
    void detectsObjectAdapterCandidate(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.adapter.Shape",
                "package fixtures.adapter;\n"
                        + "public interface Shape {\n"
                        + "    String draw();\n"
                        + "}\n",
                "fixtures.adapter.LegacyRectangle",
                "package fixtures.adapter;\n"
                        + "public class LegacyRectangle {\n"
                        + "    public String legacyDraw() { return \"rect\"; }\n"
                        + "}\n",
                "fixtures.adapter.RectangleAdapter",
                "package fixtures.adapter;\n"
                        + "public class RectangleAdapter implements Shape {\n"
                        + "    private final LegacyRectangle rectangle;\n"
                        + "    public RectangleAdapter(LegacyRectangle rectangle) {\n"
                        + "        this.rectangle = rectangle;\n"
                        + "    }\n"
                        + "    public String draw() {\n"
                        + "        return rectangle.legacyDraw();\n"
                        + "    }\n"
                        + "}\n"));

        List<Path> classFiles = List.of(
                tempDir.resolve("fixtures/adapter/Shape.class"),
                tempDir.resolve("fixtures/adapter/LegacyRectangle.class"),
                tempDir.resolve("fixtures/adapter/RectangleAdapter.class"));

        AdapterPatternLinter linter = new AdapterPatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("fixtures.adapter.RectangleAdapter"));
        assertTrue(result.contains("fixtures.adapter.Shape"));
        assertTrue(result.contains("fixtures.adapter.LegacyRectangle"));
        assertTrue(result.contains("Total adapter pattern candidates: 1"));
    }

    @Test
    void ignoresClassWithoutAdapteeDelegation(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.adapter.Shape",
                "package fixtures.adapter;\n"
                        + "public interface Shape {\n"
                        + "    String draw();\n"
                        + "}\n",
                "fixtures.adapter.PlainShape",
                "package fixtures.adapter;\n"
                        + "public class PlainShape implements Shape {\n"
                        + "    public String draw() { return \"plain\"; }\n"
                        + "}\n"));

        List<Path> classFiles = List.of(
                tempDir.resolve("fixtures/adapter/Shape.class"),
                tempDir.resolve("fixtures/adapter/PlainShape.class"));

        AdapterPatternLinter linter = new AdapterPatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("No adapter pattern candidates found."));
        assertFalse(result.contains("PlainShape"));
    }
}
