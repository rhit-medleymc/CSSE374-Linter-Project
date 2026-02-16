package datastorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import domain.LinterConfig;
import domain.SRPLinter;
import domain.TooManyParametersLinter;

class ConfigLoaderTest {

    @Test
    void loadsDefaultsWhenFileMissing(@TempDir Path tempDir) {
        ConfigLoader loader = new ConfigLoader();
        LinterConfig config = loader.loadConfig(tempDir.resolve("missing.properties").toString());

        assertEquals(LinterConfig.DEFAULT_TOO_MANY_PARAMETERS_LIMIT, config.getTooManyParametersLimit());
        assertEquals(LinterConfig.DEFAULT_SRP_LCOM_THRESHOLD, config.getSrpLcomThreshold());
        assertTrue(config.isLinterEnabled(TooManyParametersLinter.class));
        assertTrue(config.isLinterEnabled(SRPLinter.class));
    }

    @Test
    void loadsExplicitValuesFromPropertiesFile(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("linter.properties");
        Files.writeString(configFile, String.join(System.lineSeparator(),
                "enabled_linters=TooManyParametersLinter,SRPLinter",
                "too_many_parameters_limit=3",
                "srp_lcom_threshold=4"));

        ConfigLoader loader = new ConfigLoader();
        LinterConfig config = loader.loadConfig(configFile.toString());

        assertEquals(3, config.getTooManyParametersLimit());
        assertEquals(4, config.getSrpLcomThreshold());
        assertTrue(config.isLinterEnabled(TooManyParametersLinter.class));
        assertTrue(config.isLinterEnabled(SRPLinter.class));
        assertFalse(config.isLinterEnabled(domain.SingletonPatternLinter.class));
    }
}
