package domain;

import datastorage.BytecodeReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Linter that detects Single Responsibility Principle (SRP) violations
 * by analyzing class cohesion using LCOM (Lack of Cohesion of Methods) metrics.
 * 
 * Classes with LCOM scores >= threshold are flagged as potential SRP
 * violations,
 * indicating the class may have multiple responsibilities.
 */
public class SRPLinter implements Linter {

    private static final int DEFAULT_LCOM_THRESHOLD = 2;
    private final int lcomThreshold;
    private final BytecodeReader bytecodeReader;
    private final LCOMCalculator lcomCalculator;

    /**
     * Creates an SRP linter with default LCOM threshold of 2.
     * Dependencies are injected to follow Dependency Inversion Principle.
     * 
     * @param bytecodeReader the bytecode reader for loading class files
     * @param lcomCalculator the LCOM calculator for cohesion analysis
     */
    public SRPLinter(BytecodeReader bytecodeReader, LCOMCalculator lcomCalculator) {
        this(DEFAULT_LCOM_THRESHOLD, bytecodeReader, lcomCalculator);
    }

    /**
     * Creates an SRP linter with custom LCOM threshold.
     * Dependencies are injected to follow Dependency Inversion Principle.
     * 
     * @param lcomThreshold  minimum LCOM score to flag as violation (typically 2 or
     *                       higher)
     * @param bytecodeReader the bytecode reader for loading class files
     * @param lcomCalculator the LCOM calculator for cohesion analysis
     */
    public SRPLinter(int lcomThreshold, BytecodeReader bytecodeReader, LCOMCalculator lcomCalculator) {
        this.lcomThreshold = lcomThreshold;
        this.bytecodeReader = bytecodeReader;
        this.lcomCalculator = lcomCalculator;
    }

    @Override
    public String lint(List<File> files) {
        StringBuilder result = new StringBuilder();
        int violationCount = 0;
        int totalClasses = 0;

        try {
            List<ClassNode> classes = bytecodeReader.getClasses(files);

            for (ClassNode classNode : classes) {
                // Skip interfaces, enums, and anonymous classes
                if (isSkippableClass(classNode)) {
                    continue;
                }

                totalClasses++;
                LCOMCalculator.LCOMResult lcomResult = lcomCalculator.calculateLCOM(classNode);

                // Only report classes with methods and fields
                if (lcomResult.getMethodCount() > 0 && lcomResult.getFieldCount() > 0) {
                    if (lcomResult.hasLowCohesion(lcomThreshold)) {
                        violationCount++;
                        appendViolation(result, classNode, lcomResult);
                    }
                }
            }

        } catch (IOException e) {
            result.append("Error reading class files: ").append(e.getMessage())
                    .append(System.lineSeparator());
        }

        if (violationCount == 0) {
            return String.format("No SRP violations found. Analyzed %d classes.", totalClasses);
        }

        result.append(System.lineSeparator());
        result.append(String.format("Found %d SRP violation(s) in %d analyzed classes.",
                violationCount, totalClasses));
        return result.toString();
    }

    /**
     * Checks if a class should be skipped from analysis.
     */
    private boolean isSkippableClass(ClassNode classNode) {
        // Skip interfaces (they don't have instance fields typically)
        if ((classNode.access & 0x0200) != 0) { // ACC_INTERFACE
            return true;
        }

        // Skip enums
        if ((classNode.access & 0x4000) != 0) { // ACC_ENUM
            return true;
        }

        // Skip anonymous classes (contain $ in name)
        if (classNode.name.contains("$")) {
            return true;
        }

        return false;
    }

    /**
     * Formats and appends a violation report for a class.
     */
    private void appendViolation(StringBuilder result, ClassNode classNode,
            LCOMCalculator.LCOMResult lcomResult) {
        String className = classNode.name.replace('/', '.');

        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .append(System.lineSeparator());
        result.append("SRP Violation: ").append(className)
                .append(System.lineSeparator());
        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .append(System.lineSeparator());

        result.append(String.format("  LCOM Score: %d (threshold: %d)%n",
                lcomResult.getLcomScore(), lcomThreshold));
        result.append(String.format("  Methods: %d, Fields: %d%n",
                lcomResult.getMethodCount(),
                lcomResult.getFieldCount()));

        result.append(System.lineSeparator());
        result.append("  Analysis: This class has ").append(lcomResult.getLcomScore())
                .append(" disconnected component(s),").append(System.lineSeparator());
        result.append("  suggesting it may have multiple responsibilities.")
                .append(System.lineSeparator());

        // Show the components
        List<java.util.Set<String>> components = lcomResult.getComponents();
        if (components.size() > 1) {
            result.append(System.lineSeparator());
            result.append("  Suggested refactoring: Consider splitting into ")
                    .append(components.size()).append(" separate classes:")
                    .append(System.lineSeparator());

            for (int i = 0; i < components.size(); i++) {
                result.append(String.format("    Component %d: %s%n",
                        i + 1,
                        formatMethodList(components.get(i))));
            }
        }

        result.append(System.lineSeparator());
    }

    /**
     * Formats a set of method names into a readable list.
     */
    private String formatMethodList(java.util.Set<String> methods) {
        if (methods.size() <= 3) {
            return String.join(", ", methods);
        }

        // Show first 3 methods and count
        java.util.List<String> methodList = new java.util.ArrayList<>(methods);
        return String.format("%s, %s, %s... (%d methods total)",
                methodList.get(0),
                methodList.get(1),
                methodList.get(2),
                methods.size());
    }
}
