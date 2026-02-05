package datastorage;

import domain.Linter;

public class LinterLoader {

    public Linter loadLinter(String path) {
        try {
            Class<?> clazz = Class.forName(path);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Linter)) {
                throw new IllegalArgumentException(path + " does not implement Linter.");
            }
            return (Linter) instance;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load linter: " + path, e);
        }
    }
}
