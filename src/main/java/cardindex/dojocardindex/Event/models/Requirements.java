package cardindex.dojocardindex.Event.models;


import java.util.Arrays;

public enum Requirements {

    NONE("няма",0),
    TRAINER("Треньор",20),
    KYU_8("8 кю",3),
    KYU_6("6 кю",5),
    KYU_5("5 кю",6),
    KYU_4("4 кю",7),
    KYU_3("3 кю",8),
    KYU_2("2 кю",9),
    KYU_1("1 кю",10),
    DAN_1("1 дан",11);

    private final String description;
    private final int rank;

    Requirements(String description, int rank) {
        this.description = description;
        this.rank = rank;
    }

    public String getDescription() {
        return description;
    }

    public int getRank() {
        return rank;
    }

    public boolean isEqualOrHigherThan(Requirements other){
        return this.rank >= other.rank;
    }

    public static Requirements fromDescription(String description){

        return Arrays.stream(Requirements.values())
                .filter(r -> r.getDescription().equalsIgnoreCase(description)).findFirst()
                .orElse(Requirements.NONE);
    }


}
