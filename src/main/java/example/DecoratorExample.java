package example;

// Component Interface
interface Coffee {
    void brew();
}

// Concrete Component
class SimpleCoffee implements Coffee {
    public void brew() {
        System.out.println("Brewing Coffee");
    }
}

// Decorator
class CoffeeDecorator implements Coffee {
    private final Coffee coffee; // Field of same type as interface

    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }

    public void brew() {
        System.out.println("Enhanced Brewing");
        coffee.brew();
    }
}

// Another class that is NOT a decorator
class CoffeeMachine {
    private Coffee coffee; // Has field, but doesn't implement interface

    public void brew() {
        coffee.brew();
    }
}
