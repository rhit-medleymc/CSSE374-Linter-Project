package domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LinterConfig {
    public static final int DEFAULT_TOO_MANY_PARAMETERS_LIMIT = 5;
    public static final int DEFAULT_SRP_LCOM_THRESHOLD = 2;

    private final Set<String> enabledLinters;
    private final int tooManyParametersLimit;
    private final int srpLcomThreshold;

    public LinterConfig(Set<String> enabledLinters, int tooManyParametersLimit, int srpLcomThreshold) {
        this.enabledLinters = enabledLinters == null ? null : normalizeEnabledLinters(enabledLinters);
        this.tooManyParametersLimit = tooManyParametersLimit;
        this.srpLcomThreshold = srpLcomThreshold;
    }

    public static LinterConfig defaultConfig() {
        return new LinterConfig(null, DEFAULT_TOO_MANY_PARAMETERS_LIMIT, DEFAULT_SRP_LCOM_THRESHOLD);
    }

    public boolean isLinterEnabled(Class<? extends Linter> linterType) {
        if (enabledLinters == null) {
            return true;
        }
        return enabledLinters.contains(normalize(linterType.getSimpleName()));
    }

    public int getTooManyParametersLimit() {
        return tooManyParametersLimit;
    }

    public int getSrpLcomThreshold() {
        return srpLcomThreshold;
    }

    public Set<String> getEnabledLinters() {
        if (enabledLinters == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(enabledLinters);
    }

    private Set<String> normalizeEnabledLinters(Set<String> linters) {
        Set<String> normalized = new HashSet<>();
        for (String linter : linters) {
            if (linter != null && !linter.trim().isEmpty()) {
                normalized.add(normalize(linter));
            }
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
