package cardindex.dojocardindex.User.models;

public enum UserRole {

    MEMBER("член"), TRAINER("треньор"), ADMIN("Администратор");

    private final String description;


    UserRole(String description) {
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
