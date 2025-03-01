package cardindex.dojocardindex.User.models;

public enum UserStatus {
    ACTIVE("Активен"), INACTIVE("Не активен");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
