package test;

/**
 * Perfect cohesion - all methods work on the same field(s)
 */
public class GoodCohesionExample {
    private double balance;

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) {
        balance -= amount;
    }

    public double getBalance() {
        return balance;
    }

    public boolean hasPositiveBalance() {
        return balance > 0;
    }

    public void resetBalance() {
        balance = 0;
    }
}