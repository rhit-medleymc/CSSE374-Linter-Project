package presentation;

import datastorage.FileLoader;
import datastorage.LinterLoader;
import domain.ExampleLinter2;
import domain.Linter;
import domain.SnakeLinter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LinterMain {
    private final Scanner scanner;
    private final List<Linter> availableLinters;
    private final FileLoader fileLoader;
    private final LinterLoader linterLoader;

    public LinterMain() {
        this.scanner = new Scanner(System.in);
        this.availableLinters = new ArrayList<>();
        this.fileLoader = new FileLoader();
        this.linterLoader = new LinterLoader();
    }

    public static void main(String[] args) {
        new LinterMain().run();
    }

    public void loadLinters() {
        availableLinters.clear();
        availableLinters.add(new SnakeLinter());
        availableLinters.add(new ExampleLinter2());
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

        maybeLoadExternalLinter();
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

    private void maybeLoadExternalLinter() {
        System.out.println();
        System.out.print("Optional: enter a fully-qualified linter class name to load, or press Enter to skip: ");
        String className = scanner.nextLine();
        if (className == null || className.trim().isEmpty()) {
            return;
        }

        try {
            Linter loadedLinter = linterLoader.loadLinter(className.trim());
            availableLinters.add(loadedLinter);
            System.out.println("Loaded: " + className.trim());
        } catch (IllegalArgumentException ex) {
            System.out.println("Could not load custom linter. Continuing with built-in linters.");
        }
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
