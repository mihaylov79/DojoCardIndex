package cardindex.dojocardindex.exceptions;

public class UserConsentMismatchException extends RuntimeException{

    public UserConsentMismatchException(String message) {
        super(message);
    }

    public UserConsentMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
