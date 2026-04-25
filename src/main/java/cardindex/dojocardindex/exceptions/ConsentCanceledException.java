package cardindex.dojocardindex.exceptions;

public class ConsentCanceledException extends RuntimeException {

    public ConsentCanceledException(String message) {
        super(message);
    }

    public ConsentCanceledException(String message, Throwable cause) {
        super(message, cause);
    }
}
