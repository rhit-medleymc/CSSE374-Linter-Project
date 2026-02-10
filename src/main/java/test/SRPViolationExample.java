package test;

/**
 * Example class with POOR cohesion (LCOM >= 2).
 * This class violates SRP by handling both user management AND order
 * processing.
 * Methods form distinct groups that don't share fields.
 */
public class SRPViolationExample {
    // User-related fields
    private String username;
    private String email;
    private String password;

    // Order-related fields
    private int orderId;
    private double orderTotal;
    private String orderStatus;

    // User management methods (Component 1)
    public void setUserInfo(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUserEmail() {
        return email;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public boolean validateUser() {
        return username != null && password != null;
    }

    // Order processing methods (Component 2)
    public void createOrder(int id, double total) {
        this.orderId = id;
        this.orderTotal = total;
        this.orderStatus = "PENDING";
    }

    public void updateOrderStatus(String status) {
        this.orderStatus = status;
    }

    public double calculateTax() {
        return orderTotal * 0.08;
    }

    public double getFinalTotal() {
        return orderTotal + calculateTax();
    }
}
