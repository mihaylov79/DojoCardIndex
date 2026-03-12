package cardindex.dojocardindex.exceptions;

public class InvalidUserConsentException extends RuntimeException{

    public InvalidUserConsentException(String message) {
        super(message);
    }

    public InvalidUserConsentException(String message, Throwable cause) {
        super(message, cause);
    }
}
