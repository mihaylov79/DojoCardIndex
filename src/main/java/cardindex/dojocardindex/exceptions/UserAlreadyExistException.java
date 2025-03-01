package cardindex.dojocardindex.exceptions;

public class UserAlreadyExistException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Този потребител вече е регистриран";

    public UserAlreadyExistException() {
        super(DEFAULT_MESSAGE);
    }

    public UserAlreadyExistException(String message) {
        super(message);
    }
}
