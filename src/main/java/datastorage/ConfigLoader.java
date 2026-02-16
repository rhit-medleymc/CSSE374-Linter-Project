package datastorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import domain.LinterConfig;

public class ConfigLoader {

    public LinterConfig loadConfig(String path) {
        if (path == null || path.trim().isEmpty()) {
            return LinterConfig.defaultConfig();
        }

        File configFile = new File(path.trim());
        if (!configFile.exists() || !configFile.isFile()) {
            return LinterConfig.defaultConfig();
        }

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            return LinterConfig.defaultConfig();
        }

        Set<String> enabledLinters = parseEnabledLinters(properties.getProperty("enabled_linters", "all"));
        int tooManyParametersLimit = parsePositiveInt(
                properties.getProperty("too_many_parameters_limit"),
                LinterConfig.DEFAULT_TOO_MANY_PARAMETERS_LIMIT);
        int srpThreshold = parsePositiveInt(
                properties.getProperty("srp_lcom_threshold"),
                LinterConfig.DEFAULT_SRP_LCOM_THRESHOLD);

        return new LinterConfig(enabledLinters, tooManyParametersLimit, srpThreshold);
    }

    private Set<String> parseEnabledLinters(String value) {
        if (value == null || value.trim().isEmpty() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }

        Set<String> enabled = new HashSet<>();
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                enabled.add(trimmed);
            }
        }

        if (enabled.isEmpty()) {
            return null;
        }
        return enabled;
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
