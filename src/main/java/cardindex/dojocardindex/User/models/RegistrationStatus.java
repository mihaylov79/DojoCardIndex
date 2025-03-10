package cardindex.dojocardindex.User.models;



public enum RegistrationStatus {

    REGISTERED("Регистриран"), NOT_REGISTERED("Няма регистрация"), PENDING("Чака потвърждение");

    private final String description;

    RegistrationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
