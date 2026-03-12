package cardindex.dojocardindex.UserConsent.model;

public enum MailSendStatus {

    SENT("Изпратен"),
    FAILED("Неуспешно"),
    UNNECESSARY("Ненужно");

    private final String description;

    MailSendStatus(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
