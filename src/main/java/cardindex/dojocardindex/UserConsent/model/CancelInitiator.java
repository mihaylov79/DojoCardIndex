package cardindex.dojocardindex.UserConsent.model;

public enum CancelInitiator {
    USER("Потребител"),
    PARENT("Родител(настойник)"),
    ADMIN("Администратор");

    private final String description;

    CancelInitiator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
