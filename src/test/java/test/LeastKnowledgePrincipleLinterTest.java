package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import domain.LeastKnowledgePrincipleLinter;

public class LeastKnowledgePrincipleLinterTest {

    @Test
    void detectsMethodChainsThatBreakDemeter(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("DemeterExample.java");
        Files.writeString(testFile, String.join(System.lineSeparator(),
                "public class DemeterExample {",
                "    void ok() {",
                "        helper();",
                "        System.out.println(\"no chain\");",
                "    }",
                "    void bad() {",
                "        A a = new A();",
                "        a.getB().doSomething();", // should be flagged (method-call result used)
                "        a.getB().getC().doIt();", // should be flagged (chained calls)
                "        this.getA().b.c();", // should be flagged (chained field access)
                "    }",
                "}"));

        LeastKnowledgePrincipleLinter linter = new LeastKnowledgePrincipleLinter();
        String result = linter.lint(List.of(testFile.toFile()));
        System.out.println("Linter result:\n" + result);

        assertTrue(result.contains("DemeterExample.java"));
        assertTrue(result.contains("Line 8"), "should flag first chained call on line 8");
        assertTrue(result.contains("Line 9"), "should flag second chained call on line 9");
        assertTrue(result.contains("Line 10"), "should flag chained field access on line 10");
        assertTrue(result.contains("Total least knowledge principle issues: 4"));
    }
}
