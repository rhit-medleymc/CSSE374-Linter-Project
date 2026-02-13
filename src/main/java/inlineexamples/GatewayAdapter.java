package inlineexamples;

public class GatewayAdapter implements PaymentTarget {
    private final LegacyGateway legacyGateway;

    public GatewayAdapter(LegacyGateway legacyGateway) {
        this.legacyGateway = legacyGateway;
    }

    @Override
    public String pay(int cents) {
        return legacyGateway.chargeLegacy(cents);
    }
}
