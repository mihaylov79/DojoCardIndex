package cardindex.dojocardindex.Event.models;

public enum EventType {

    TOURNAMENT("Турнир"),
    TRAINING_CAMP("Спортен лагер"),
    SEMINAR("Семинар");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
