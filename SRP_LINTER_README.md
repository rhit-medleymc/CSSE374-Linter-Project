# SRP Linter - Quick Reference

## What It Does
Detects Single Responsibility Principle violations by calculating LCOM (Lack of Cohesion of Methods) scores from Java bytecode.

## How It Works
- **LCOM Score = 1**: Highly cohesive class ✅
- **LCOM Score ≥ 2**: Low cohesion, potential SRP violation ⚠️
- Higher scores = more distinct responsibilities

## Usage

### Run the Linter
```bash
# Build the project
mvn clean package

# Run the linter
java -jar target/LinterProject-1.0-rc3-jar-with-dependencies.jar

# When prompted, enter path to analyze:
target/classes/example
```

### Customize Threshold
Edit `LinterMain.java` line 35:
```java
availableLinters.add(new SRPLinter(3)); // Only flag LCOM >= 3
```

## Example Output
```
SRP Violation: example.SRPViolationExample
  LCOM Score: 2 (threshold: 2)
  Methods: 8, Fields: 6
  
  Suggested refactoring: Consider splitting into 2 separate classes:
    Component 1: setUserInfo, getUserEmail, validateUser... (4 methods)
    Component 2: createOrder, getFinalTotal, calculateTax... (4 methods)
```

## Test Examples
- `GoodCohesionExample.java` - Cohesive person class
- `SRPViolationExample.java` - User + Order management (LCOM=2)
- `MultipleSRPViolationExample.java` - User + Product + Email (LCOM=3)

## Key Files
- `LCOMCalculator.java` - LCOM4 algorithm implementation
- `SRPLinter.java` - Main linter logic
- `LinterMain.java` - Integration point
