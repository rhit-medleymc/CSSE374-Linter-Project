package test;

/**
 * Example class with SEVERE SRP violation (LCOM >= 3).
 * This class handles user data, product inventory, AND email notifications.
 * Three completely separate responsibilities with no shared fields.
 */
public class MultipleSRPViolationExample {
    // User data fields
    private String userId;
    private String userName;

    // Product inventory fields
    private int productId;
    private int stockQuantity;
    private double productPrice;

    // Email notification fields
    private String emailSubject;
    private String emailBody;
    private String recipientEmail;

    // User data methods (Component 1)
    public void createUser(String id, String name) {
        this.userId = id;
        this.userName = name;
    }

    public String getUserInfo() {
        return userId + ": " + userName;
    }

    // Product inventory methods (Component 2)
    public void addProduct(int id, int quantity, double price) {
        this.productId = id;
        this.stockQuantity = quantity;
        this.productPrice = price;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public double calculateInventoryValue() {
        return stockQuantity * productPrice;
    }

    // Email notification methods (Component 3)
    public void composeEmail(String subject, String body, String recipient) {
        this.emailSubject = subject;
        this.emailBody = body;
        this.recipientEmail = recipient;
    }

    public String getEmailPreview() {
        return "To: " + recipientEmail + "\nSubject: " + emailSubject;
    }

    public boolean isEmailReady() {
        return emailSubject != null && emailBody != null && recipientEmail != null;
    }
}
