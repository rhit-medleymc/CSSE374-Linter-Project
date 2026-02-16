// Jasmeen
package domain;

import datastorage.ASMReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

/**
 * Linter that detects Single Responsibility Principle (SRP) violations
 * by analyzing class cohesion using LCOM (Lack of Cohesion of Methods) metrics.
 * 
 * Classes with LCOM scores >= threshold are flagged as potential SRP
 * violations,
 * indicating the class may have multiple responsibilities.
 */
public class SRPLinter extends AbstractASMLinter {

    private static final int DEFAULT_LCOM_THRESHOLD = 2;
    private final int lcomThreshold;
    private final LCOMCalculator lcomCalculator;

    /**
     * Creates an SRP linter with default LCOM threshold of 2.
     * Dependencies are injected to follow Dependency Inversion Principle.
     * 
     * @param asmReader      the bytecode reader for loading class files
     * @param lcomCalculator the LCOM calculator for cohesion analysis
     */
    public SRPLinter(ASMReader asmReader, LCOMCalculator lcomCalculator) {
        this(DEFAULT_LCOM_THRESHOLD, asmReader, lcomCalculator);
    }

    /**
     * Creates an SRP linter with custom LCOM threshold.
     * Dependencies are injected to follow Dependency Inversion Principle.
     * 
     * @param lcomThreshold  minimum LCOM score to flag as violation (typically 2 or
     *                       higher)
     * @param asmReader      the bytecode reader for loading class files
     * @param lcomCalculator the LCOM calculator for cohesion analysis
     */
    public SRPLinter(int lcomThreshold, ASMReader asmReader, LCOMCalculator lcomCalculator) {
        super(asmReader);
        this.lcomThreshold = lcomThreshold;
        this.lcomCalculator = lcomCalculator;
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        LCOMCalculator.LCOMResult lcomResult = lcomCalculator.calculateLCOM(classNode);

        // Only report classes with methods and fields
        if (lcomResult.getMethodCount() > 0 && lcomResult.getFieldCount() > 0) {
            if (lcomResult.hasLowCohesion(lcomThreshold)) {
                appendViolation(result, classNode, lcomResult);
                return 1;
            }
        }
        return 0;
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

    @Override
    protected String getNoViolationsMessage() {
        return "No SRP violations found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        // SRPLinter had a specific summary format, but leveraging the template standard
        // is fine too.
        // The original had:
        // Found %d SRP violation(s) in %d analyzed classes.
        // Since we don't have totalClasses easily, we'll just return the result
        // accumulator which contains the formatted violations.
        // We can prepend the count if we want to match AbstractASMLinter's default
        // style,
        // or just return the result if the individual violations are detailed enough
        // (which they are).
        // Let's stick to returning the result as SRPLinter's violations are very large
        // blocks.
        // Actually, let's prepend a summary line.
        return System.lineSeparator() +
                String.format("Found %d SRP violation(s).", violationCount) +
                System.lineSeparator() +
                result.toString();
    }
}
