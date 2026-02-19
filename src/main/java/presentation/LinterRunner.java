package presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import domain.AdapterPatternLinter;
import domain.BooleanFlagMethodLinter;
import domain.DecoratorPatternLinter;
import domain.DesignRiskLinter;
import domain.FacadePatternLinter;
import domain.Linter;
import domain.PlantUMLGenerator;
import domain.PublicNonFinalFieldLinter;
import domain.SRPLinter;
import domain.SingletonPatternLinter;
import domain.SnakeLinter;
import domain.StrategyPatternLinter;
import domain.TooManyParametersLinter;
import domain.TrailingWhitespaceLinter;
import domain.UnusedImportLinter;

public class LinterRunner {

    private static final List<Class<? extends Linter>> CLASS_FILE_LINTER_TYPES = List.of(
            SRPLinter.class,
            FacadePatternLinter.class,
            StrategyPatternLinter.class,
            SingletonPatternLinter.class,
            DecoratorPatternLinter.class,
            AdapterPatternLinter.class,
            BooleanFlagMethodLinter.class,
            PublicNonFinalFieldLinter.class,
            TooManyParametersLinter.class,
            PlantUMLGenerator.class,
            DesignRiskLinter.class);
    private static final List<Class<? extends Linter>> NON_CLASS_FILE_LINTER_TYPES = List.of(
            SnakeLinter.class,
            UnusedImportLinter.class,
            TrailingWhitespaceLinter.class);

    public String run(List<File> files, List<Linter> availableLinters) {
        List<File> classFiles = new ArrayList<>();
        List<File> nonClassFiles = new ArrayList<>();
        splitFilesByType(files, classFiles, nonClassFiles);

        return runLintBatches(classFiles, nonClassFiles, availableLinters);
    }

    private void splitFilesByType(List<File> files, List<File> classFiles, List<File> nonClassFiles) {
        for (File file : files) {
            if (isClassFile(file)) {
                classFiles.add(file);
            } else {
                nonClassFiles.add(file);
            }
        }
    }

    private boolean isClassFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".class");
    }

    private String runLintBatches(List<File> classFiles, List<File> nonClassFiles, List<Linter> availableLinters) {
        StringBuilder output = new StringBuilder();

        if (!classFiles.isEmpty()) {
            output.append("=== .class Files ===").append(System.lineSeparator());
            output.append(runLinters(selectLinters(CLASS_FILE_LINTER_TYPES, availableLinters), classFiles));
        }

        if (!nonClassFiles.isEmpty()) {
            if (output.length() > 0) {
                output.append(System.lineSeparator());
            }
            output.append("=== Non-.class Files ===").append(System.lineSeparator());
            output.append(runLinters(selectLinters(NON_CLASS_FILE_LINTER_TYPES, availableLinters), nonClassFiles));
        }

        if (output.length() == 0) {
            return "No files to lint.";
        }

        return output.toString();
    }

    private List<Linter> selectLinters(List<Class<? extends Linter>> linterTypes, List<Linter> availableLinters) {
        List<Linter> selected = new ArrayList<>();
        for (Class<? extends Linter> linterType : linterTypes) {
            for (Linter linter : availableLinters) {
                if (linterType.isInstance(linter)) {
                    selected.add(linter);
                    break;
                }
            }
        }
        return selected;
    }

    private String runLinters(List<Linter> linters, List<File> files) {
        if (linters.isEmpty()) {
            return "No linters configured for this file type." + System.lineSeparator();
        }

        StringBuilder output = new StringBuilder();
        for (Linter linter : linters) {
            output.append("[").append(linter.getClass().getSimpleName()).append("]").append(System.lineSeparator());
            output.append(linter.lint(files)).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return output.toString();
    }
}
