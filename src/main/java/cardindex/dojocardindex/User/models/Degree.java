package cardindex.dojocardindex.User.models;

import java.util.Arrays;

public enum Degree {
    NONE("не е посочена",0),
    KYU_10("10 кю",1),
    KYU_9("9 кю",2),
    KYU_8("8 кю",3),
    KYU_7("7 кю",4),
    KYU_6("6 кю",5),
    KYU_5("5 кю",6),
    KYU_4("4 кю",7),
    KYU_3("3 кю",8),
    KYU_2("2 кю",9),
    KYU_1("1 кю",10),
    DAN_1("1 дан",11),
    DAN_2("2 дан",12),
    DAN_3("3 дан",13),
    DAN_4("4 дан",14),
    DAN_5("5 дан",15),
    DAN_6("6 дан",16),
    DAN_7("7 дан",17),
    DAN_8("8 дан",18),
    DAN_9("9 дан",19);


    private final String description;
    private final int rank;


    Degree(String description, int rank) {
        this.description = description;
        this.rank = rank;
    }


    public String getDescription() {
        return description;
    }

    public int getRank() {
        return rank;
    }

    public boolean isEqualOrHigherThan(Degree other){
        return this.rank >= other.getRank();
    }

    public static Degree fromDescription(String degree){
        return Arrays.stream(Degree.values())
                .filter(d -> d.getDescription().equalsIgnoreCase(degree))
                .findFirst().orElse(Degree.NONE);
    }
}
