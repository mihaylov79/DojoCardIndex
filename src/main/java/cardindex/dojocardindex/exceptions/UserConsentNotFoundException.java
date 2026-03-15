package cardindex.dojocardindex.exceptions;

public class UserConsentNotFoundException extends RuntimeException {

    public UserConsentNotFoundException(String message) {
        super(message);


    }
    public UserConsentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

