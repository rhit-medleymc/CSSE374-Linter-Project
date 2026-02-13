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

class FacadePatternLinterTest {

    @Test
    void detectsFacadeCandidate(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.facade.PaymentGateway",
                "package fixtures.facade;\n"
                        + "public class PaymentGateway {\n"
                        + "    public void charge(int cents) {}\n"
                        + "}\n",
                "fixtures.facade.EmailService",
                "package fixtures.facade;\n"
                        + "public class EmailService {\n"
                        + "    public void sendReceipt() {}\n"
                        + "}\n",
                "fixtures.facade.CheckoutFacade",
                "package fixtures.facade;\n"
                        + "public class CheckoutFacade {\n"
                        + "    private final PaymentGateway gateway = new PaymentGateway();\n"
                        + "    private final EmailService emailService = new EmailService();\n"
                        + "    public void checkout(int cents) {\n"
                        + "        gateway.charge(cents);\n"
                        + "        emailService.sendReceipt();\n"
                        + "    }\n"
                        + "    public void resendReceipt() {\n"
                        + "        emailService.sendReceipt();\n"
                        + "    }\n"
                        + "}\n"));

        List<Path> classFiles = List.of(
                tempDir.resolve("fixtures/facade/PaymentGateway.class"),
                tempDir.resolve("fixtures/facade/EmailService.class"),
                tempDir.resolve("fixtures/facade/CheckoutFacade.class"));

        FacadePatternLinter linter = new FacadePatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("fixtures.facade.CheckoutFacade"));
        assertTrue(result.contains("Total facade pattern candidates: 1"));
    }

    @Test
    void ignoresSimpleWrapperThatOnlyDelegatesToOneSubsystem(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.facade.PaymentGateway",
                "package fixtures.facade;\n"
                        + "public class PaymentGateway {\n"
                        + "    public void charge(int cents) {}\n"
                        + "}\n",
                "fixtures.facade.SingleSubsystemWrapper",
                "package fixtures.facade;\n"
                        + "public class SingleSubsystemWrapper {\n"
                        + "    private final PaymentGateway gateway = new PaymentGateway();\n"
                        + "    public void checkout(int cents) {\n"
                        + "        gateway.charge(cents);\n"
                        + "    }\n"
                        + "}\n"));

        List<Path> classFiles = List.of(
                tempDir.resolve("fixtures/facade/PaymentGateway.class"),
                tempDir.resolve("fixtures/facade/SingleSubsystemWrapper.class"));

        FacadePatternLinter linter = new FacadePatternLinter();
        String result = linter.lint(classFiles.stream().map(Path::toFile).collect(Collectors.toList()));

        assertTrue(result.contains("No facade pattern candidates found."));
        assertFalse(result.contains("SingleSubsystemWrapper"));
    }
}
