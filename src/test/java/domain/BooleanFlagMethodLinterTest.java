package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testsupport.JavaClassFixtureCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooleanFlagMethodLinterTest {

    @Test
    void detectsMethodsWithBooleanFlagParameters(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.booleanflag.CheckoutService",
                "package fixtures.booleanflag;\n"
                        + "public class CheckoutService {\n"
                        + "    public void submitOrder(boolean express, int itemCount) {}\n"
                        + "    public void process(int itemCount) {}\n"
                        + "    public void setEnabled(Boolean enabled) {}\n"
                        + "}\n"));

        Path classFile = tempDir.resolve("fixtures/booleanflag/CheckoutService.class");

        BooleanFlagMethodLinter linter = new BooleanFlagMethodLinter();
        String result = linter.lint(List.of(classFile.toFile()));

        assertTrue(result.contains("submitOrder"));
        assertTrue(result.contains("setEnabled"));
        assertTrue(result.contains("Total boolean flag method issues: 2"));
    }

    @Test
    void ignoresMethodsWithoutBooleanFlagParameters(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.booleanflag.PlainService",
                "package fixtures.booleanflag;\n"
                        + "public class PlainService {\n"
                        + "    public void submitOrder(int itemCount, String note) {}\n"
                        + "}\n"));

        Path classFile = tempDir.resolve("fixtures/booleanflag/PlainService.class");

        BooleanFlagMethodLinter linter = new BooleanFlagMethodLinter();
        String result = linter.lint(List.of(classFile.toFile()));

        assertTrue(result.contains("No boolean flag method issues found."));
        assertFalse(result.contains("submitOrder"));
    }
}
