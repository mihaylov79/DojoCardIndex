package cardindex.dojocardindex.User.models;

public enum AgeGroup {

    CH8("деца до 8 г."),
    CH10("деца до 10 г."),
    CH12("деца до 12 г."),
    CH14("деца до 14 г."),
    U16("юноши младша възраст - до 16 г."),
    U18("юноши старша възраст - до 18 г."),
    M("мъже - над 18 г."),
    F("жени - над 18 г.");

    private final String description;

    AgeGroup(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
