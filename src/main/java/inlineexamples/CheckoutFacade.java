package inlineexamples;

public class CheckoutFacade {
    private final BillingSubsystem billingSubsystem;
    private final NotificationSubsystem notificationSubsystem;

    public CheckoutFacade(BillingSubsystem billingSubsystem, NotificationSubsystem notificationSubsystem) {
        this.billingSubsystem = billingSubsystem;
        this.notificationSubsystem = notificationSubsystem;
    }

    public void checkout(int cents) {
        billingSubsystem.charge(cents);
        notificationSubsystem.sendReceipt();
    }

    public void resendReceipt() {
        notificationSubsystem.sendReceipt();
    }
}
