package cardindex.dojocardindex.UserConsent.model;

public enum MailSendStatus {

    SENT("Изпратен"),
    INVITATION_FAILED("Неуспешно изпращане (покана)"),
    CONFIRMATION_FAILED("Неуспешно изпращане (потвърждение)"),
    FAILED("Неуспешно (legacy)"),
    UNNECESSARY("Ненужно");

    private final String description;

    MailSendStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
