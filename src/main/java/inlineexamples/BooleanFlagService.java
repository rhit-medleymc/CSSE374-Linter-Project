package inlineexamples;

public class BooleanFlagService {
    public String renderInvoice(boolean includeTaxes) {
        if (includeTaxes) {
            return "with-tax";
        }
        return "without-tax";
    }

    public void setNotificationEnabled(Boolean enabled) {
        if (enabled == null) {
            return;
        }
    }
}
