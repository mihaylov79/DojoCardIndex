package cardindex.dojocardindex.User.models;

public enum Achievments {
    PLACE_1("1-во място"),
    PLACE_2("2-ро място"),
    PLACE_3("3-то място");

    private final String description;

    Achievments(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
