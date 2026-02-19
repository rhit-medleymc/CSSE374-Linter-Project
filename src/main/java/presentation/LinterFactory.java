package presentation;

import java.util.ArrayList;
import java.util.List;

import domain.AdapterPatternLinter;
import domain.BooleanFlagMethodLinter;
import domain.DecoratorPatternLinter;
import domain.DesignRiskLinter;
import domain.FacadePatternLinter;
import domain.Linter;
import domain.LinterConfig;
import domain.PlantUMLGenerator;
import domain.PublicNonFinalFieldLinter;
import domain.SRPLinter;
import domain.SingletonPatternLinter;
import domain.SnakeLinter;
import domain.StrategyPatternLinter;
import domain.TooManyParametersLinter;
import domain.TrailingWhitespaceLinter;
import domain.UnusedImportLinter;

public class LinterFactory {

    public List<Linter> createLinters(LinterConfig config) {
        List<Linter> linters = new ArrayList<>();

        // Create data layer dependencies (shared across linters if needed)
        datastorage.ASMReader asmReader = new datastorage.ASMReader();

        // Create domain layer utilities
        domain.LCOMCalculator lcomCalculator = new domain.LCOMCalculator();

        // Add linters with dependency injection and config-driven thresholds
        addIfEnabled(linters, new SnakeLinter(), SnakeLinter.class, config);
        addIfEnabled(linters, new TrailingWhitespaceLinter(), TrailingWhitespaceLinter.class, config);
        addIfEnabled(linters, new PublicNonFinalFieldLinter(asmReader), PublicNonFinalFieldLinter.class, config);
        addIfEnabled(linters, new SRPLinter(config.getSrpLcomThreshold(), asmReader, lcomCalculator), SRPLinter.class,
                config);
        addIfEnabled(linters, new FacadePatternLinter(asmReader), FacadePatternLinter.class, config);
        addIfEnabled(linters, new StrategyPatternLinter(), StrategyPatternLinter.class, config);
        addIfEnabled(linters, new SingletonPatternLinter(asmReader), SingletonPatternLinter.class, config);
        addIfEnabled(linters, new DecoratorPatternLinter(asmReader), DecoratorPatternLinter.class, config);
        addIfEnabled(linters, new AdapterPatternLinter(asmReader), AdapterPatternLinter.class, config);
        addIfEnabled(linters, new BooleanFlagMethodLinter(asmReader), BooleanFlagMethodLinter.class, config);
        addIfEnabled(linters, new PlantUMLGenerator(), PlantUMLGenerator.class, config);
        addIfEnabled(linters, new UnusedImportLinter(), UnusedImportLinter.class, config);
        addIfEnabled(linters, new TooManyParametersLinter(config.getTooManyParametersLimit(), asmReader),
                TooManyParametersLinter.class, config);
        addIfEnabled(linters, new DesignRiskLinter(asmReader, lcomCalculator), DesignRiskLinter.class, config);

        return linters;
    }

    private void addIfEnabled(List<Linter> linters, Linter linter, Class<? extends Linter> linterType,
            LinterConfig config) {
        if (config.isLinterEnabled(linterType)) {
            linters.add(linter);
        }
    }
}
