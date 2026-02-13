package presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import datastorage.FileLoader;
import domain.Linter;
import domain.AdapterPatternLinter;
import domain.BooleanFlagMethodLinter;
import domain.FacadePatternLinter;
import domain.PlantUMLGenerator;
import domain.PublicNonFinalFieldLinter;
import domain.SRPLinter;
import domain.SnakeLinter;
import domain.TooManyParametersLinter;
import domain.TrailingWhitespaceLinter;

public class LinterMain {
    // Update these lists to change which linters run for each file category.
    private static final List<Class<? extends Linter>> CLASS_FILE_LINTER_TYPES = List.of(
            SRPLinter.class,
            FacadePatternLinter.class,
            AdapterPatternLinter.class,
            BooleanFlagMethodLinter.class,
            PlantUMLGenerator.class);
    private static final List<Class<? extends Linter>> NON_CLASS_FILE_LINTER_TYPES = List.of(
            SnakeLinter.class,
            TrailingWhitespaceLinter.class,
            PublicNonFinalFieldLinter.class);

    private final List<Linter> availableLinters;
    private final FileLoader fileLoader;
    private final Scanner scanner;

    public LinterMain() {
        this.availableLinters = new ArrayList<>();
        this.fileLoader = new FileLoader();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        new LinterMain().run();
    }

    public void loadLinters() {
        availableLinters.clear();

        // Create data layer dependencies (shared across linters if needed)
        datastorage.ASMReader asmReader = new datastorage.ASMReader();

        // Create domain layer utilities
        domain.LCOMCalculator lcomCalculator = new domain.LCOMCalculator();

        // Add linters with dependency injection
        availableLinters.add(new SnakeLinter());
        availableLinters.add(new TrailingWhitespaceLinter());
        availableLinters.add(new PublicNonFinalFieldLinter());
        availableLinters.add(new SRPLinter(asmReader, lcomCalculator));
        availableLinters.add(new FacadePatternLinter());
        availableLinters.add(new AdapterPatternLinter());
        availableLinters.add(new BooleanFlagMethodLinter());
        availableLinters.add(new PlantUMLGenerator());
        availableLinters.add(new TooManyParametersLinter());
    }

    public void run() {
        loadLinters();
        System.out.println("=== Base Linter (Terminal) ===");
        System.out.println("Built-in linter #1 checks for trailing whitespace.");

        String fileInput = askForFileInput();
        List<File> requestedPaths = fileLoader.loadFiles(fileInput);
        if (requestedPaths.isEmpty()) {
            System.out.println("No files provided. Exiting.");
            return;
        }

        List<File> files = expandInputFiles(requestedPaths);
        if (files.isEmpty()) {
            System.out.println("No valid files found. Exiting.");
            return;
        }

        List<File> classFiles = new ArrayList<>();
        List<File> nonClassFiles = new ArrayList<>();
        splitFilesByType(files, classFiles, nonClassFiles);

        String result = runLintBatches(classFiles, nonClassFiles);
        displayResult(result);
    }

    private String askForFileInput() {
        System.out.println();
        System.out.println("Enter file or folder path(s) to lint.");
        System.out.println("Use comma-separated paths for multiple entries.");
        System.out.print("> ");
        return scanner.nextLine();
    }

    private void displayResult(String result) {
        System.out.println();
        System.out.println("=== Lint Results ===");
        System.out.println(result);
    }

    private List<File> expandInputFiles(List<File> requestedPaths) {
        List<File> files = new ArrayList<>();
        Set<String> seenPaths = new LinkedHashSet<>();

        for (File path : requestedPaths) {
            if (!path.exists()) {
                System.out.println("Skipping missing path: " + path.getPath());
                continue;
            }
            collectFiles(path, files, seenPaths);
        }

        return files;
    }

    private void collectFiles(File path, List<File> files, Set<String> seenPaths) {
        if (path.isFile()) {
            String absolutePath = path.getAbsolutePath();
            if (seenPaths.add(absolutePath)) {
                files.add(path);
            }
            return;
        }

        if (!path.isDirectory()) {
            return;
        }

        File[] children = path.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        for (File child : children) {
            collectFiles(child, files, seenPaths);
        }
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

    private String runLintBatches(List<File> classFiles, List<File> nonClassFiles) {
        StringBuilder output = new StringBuilder();

        if (!classFiles.isEmpty()) {
            output.append("=== .class Files ===").append(System.lineSeparator());
            output.append(runLinters(selectLinters(CLASS_FILE_LINTER_TYPES), classFiles));
        }

        if (!nonClassFiles.isEmpty()) {
            if (output.length() > 0) {
                output.append(System.lineSeparator());
            }
            output.append("=== Non-.class Files ===").append(System.lineSeparator());
            output.append(runLinters(selectLinters(NON_CLASS_FILE_LINTER_TYPES), nonClassFiles));
        }

        if (output.length() == 0) {
            return "No files to lint.";
        }

        return output.toString();
    }

    private List<Linter> selectLinters(List<Class<? extends Linter>> linterTypes) {
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
