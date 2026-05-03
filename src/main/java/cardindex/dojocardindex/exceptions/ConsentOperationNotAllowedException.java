package cardindex.dojocardindex.exceptions;

public class ConsentOperationNotAllowedException extends RuntimeException{

    public ConsentOperationNotAllowedException(String message) {
        super(message);
    }

    public ConsentOperationNotAllowedException(Throwable cause) {
        super(cause);
    }
}
