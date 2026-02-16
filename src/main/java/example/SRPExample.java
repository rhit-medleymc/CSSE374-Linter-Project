package example;

/**
 * SRP Violation Example:
 * This class handles both User Management and Email Notifications.
 * The methods for User use the 'name' field.
 * The methods for Email use the 'emailServer' field.
 * These two sets of methods/fields are disconnected -> Low Cohesion -> SRP
 * Violation.
 */
public class SRPExample {

    // Responsibility 1: User Data
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Responsibility 2: Email Sending
    private String emailServer;

    public void configureEmail(String server) {
        this.emailServer = server;
    }

    public void sendEmail(String message) {
        System.out.println("Sending " + message + " via " + emailServer);
    }
}
