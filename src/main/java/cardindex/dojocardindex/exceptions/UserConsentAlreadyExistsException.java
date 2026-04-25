package cardindex.dojocardindex.exceptions;

public class UserConsentAlreadyExistsException extends RuntimeException{

    public UserConsentAlreadyExistsException(String message) {
        super(message);
    }

    public UserConsentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
