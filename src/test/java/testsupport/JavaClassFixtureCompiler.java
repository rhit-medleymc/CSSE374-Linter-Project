package testsupport;

import org.junit.jupiter.api.Assertions;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class JavaClassFixtureCompiler {
    private JavaClassFixtureCompiler() {
    }

    public static void compileTo(Path outputDirectory, Map<String, String> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assertions.assertNotNull(compiler, "JDK compiler is required to run these tests.");

        Files.createDirectories(outputDirectory);
        Path sourceDirectory = outputDirectory.resolve("src");
        Files.createDirectories(sourceDirectory);

        List<Path> sourcePaths = new ArrayList<>();
        for (Map.Entry<String, String> sourceEntry : sources.entrySet()) {
            String className = sourceEntry.getKey();
            String sourceCode = sourceEntry.getValue();

            Path sourceFile = sourceDirectory.resolve(className.replace('.', '/') + ".java");
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, sourceCode);
            sourcePaths.add(sourceFile);
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(
                            sourcePaths.stream().map(Path::toFile).collect(Collectors.toList()));

            List<String> options = List.of("-d", outputDirectory.toAbsolutePath().toString());
            Boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

            if (!Boolean.TRUE.equals(success)) {
                Assertions.fail("Fixture compilation failed:\n" + diagnostics.getDiagnostics());
            }
        }
    }
}
