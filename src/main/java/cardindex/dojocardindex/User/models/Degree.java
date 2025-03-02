package cardindex.dojocardindex.User.models;

public enum Degree {
    KYU_10("10 кю"),
    KYU_9("9 кю"),
    KYU_8("8 кю"),
    KYU_7("7 кю"),
    KYU_6("6 кю"),
    KYU_5("5 кю"),
    KYU_4("4 кю"),
    KYU_3("3 кю"),
    KYU_2("2 кю"),
    KYU_1("1 кю"),
    DAN_1("1 дан"),
    DAN_2("2 дан"),
    DAN_3("3 дан"),
    DAN_4("4 дан"),
    DAN_5("5 дан"),
    DAN_6("6 дан"),
    DAN_7("7 дан"),
    DAN_8("8 дан"),
    DAN_9("9 дан");


    private final String description;


    Degree(String description) {
        this.description = description;
    }


    public String getDescription() {
        return description;
    }


}
