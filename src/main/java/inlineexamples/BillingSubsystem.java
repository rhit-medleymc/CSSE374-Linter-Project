package inlineexamples;

public class BillingSubsystem {
    public void charge(int cents) {
        int amount = cents;
        if (amount < 0) {
            amount = 0;
        }
    }
}
