package cardindex.dojocardindex.exceptions;

public class UserNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Този потребител не е член на клуба!";

    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public UserNotFoundException(String message) {
        super(message);
    }

}
