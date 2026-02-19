package example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import datastorage.ConfigLoader;
import datastorage.FileLoader;
import domain.Linter;
import domain.LinterConfig;
import presentation.LinterFactory;
import presentation.LinterRunner;

public class LinterMainExample {
    private static final String DEFAULT_CONFIG_PATH = "linter.properties";

    private final List<Linter> availableLinters;
    private final FileLoader fileLoader;
    private final ConfigLoader configLoader;
    private final Scanner scanner;
    private final LinterFactory linterFactory;
    private final LinterRunner linterRunner;

    public LinterMainExample() {
        this.availableLinters = new ArrayList<>();
        this.fileLoader = new FileLoader();
        this.configLoader = new ConfigLoader();
        this.scanner = new Scanner(System.in);
        this.linterFactory = new LinterFactory();
        this.linterRunner = new LinterRunner();
    }

    public static void main(String[] args) {
        new LinterMainExample().run();
    }

    public void run() {
        LinterConfig config = configLoader.loadConfig(DEFAULT_CONFIG_PATH);
        availableLinters.addAll(linterFactory.createLinters(config));

        System.out.println("=== Base Linter (Terminal) ===");
        System.out.println("Built-in linter #1 checks for trailing whitespace.");
        System.out.println("Config file: " + DEFAULT_CONFIG_PATH);

        String fileInput = askForFileInput();
        List<File> files = fileLoader.loadFiles(fileInput);
        if (files.isEmpty()) {
            System.out.println("No valid files found. Exiting.");
            return;
        }

        String result = linterRunner.run(files, availableLinters);
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
}
