package cardindex.dojocardindex.Event.models;

public enum Requirements {

    NONE("няма"),
    TRAINER("Треньор"),
    KYU_8("8 кю"),
    KYU_6("6 кю"),
    KYU_5("5 кю"),
    KYU_4("4 кю"),
    KYU_3("3 кю"),
    KYU_2("2 кю"),
    KYU_1("1 кю"),
    DAN_1("1 дан");

    private final String description;

    Requirements(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
