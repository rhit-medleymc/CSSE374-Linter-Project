package presentation;

import datastorage.FileLoader;
import domain.ExampleLinter2;
import domain.Linter;
import domain.PublicNonFinalFieldLinter;
import domain.SRPLinter;
import domain.SnakeLinter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LinterMain {
    private final Scanner scanner;
    private final List<Linter> availableLinters;
    private final FileLoader fileLoader;

    public LinterMain() {
        this.scanner = new Scanner(System.in);
        this.availableLinters = new ArrayList<>();
        this.fileLoader = new FileLoader();
    }

    public static void main(String[] args) {
        new LinterMain().run();
    }

    public void loadLinters() {
        availableLinters.clear();

        // Create data layer dependencies (shared across linters if needed)
        datastorage.BytecodeReader bytecodeReader = new datastorage.BytecodeReader();

        // Create domain layer utilities
        domain.LCOMCalculator lcomCalculator = new domain.LCOMCalculator();

        // Add linters with dependency injection
        availableLinters.add(new SnakeLinter());
        availableLinters.add(new ExampleLinter2());
        availableLinters.add(new PublicNonFinalFieldLinter());
        availableLinters.add(new SRPLinter(bytecodeReader, lcomCalculator));
    }

    public void run() {
        loadLinters();
        System.out.println("=== Base Linter (Terminal) ===");
        System.out.println("Built-in linter #1 checks for trailing whitespace.");

        String fileInput = askForFileInput();
        List<File> files = fileLoader.loadFiles(fileInput);
        if (files.isEmpty()) {
            System.out.println("No files provided. Exiting.");
            return;
        }

        String result = callLinters(availableLinters, files);
        displayResult(result);
    }

    private String askForFileInput() {
        System.out.println();
        System.out.println("Enter file path(s) to lint.");
        System.out.println("Use comma-separated paths for multiple files.");
        System.out.print("> ");
        return scanner.nextLine();
    }

    private String callLinters(List<Linter> linters, List<File> files) {
        StringBuilder output = new StringBuilder();
        for (Linter linter : linters) {
            output.append("[").append(linter.getClass().getSimpleName()).append("]").append(System.lineSeparator());
            output.append(linter.lint(files)).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return output.toString();
    }

    public void displayResult(String result) {
        System.out.println();
        System.out.println("=== Lint Results ===");
        System.out.println(result);
    }
}
