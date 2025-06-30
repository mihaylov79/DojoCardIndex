package cardindex.dojocardindex.EventParticipationRequest.model;

public enum RequestStatus {

    PENDING("Чакаща"),
    APPROVED("Одобрена"),
    REJECTED("Отхвърлена");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
