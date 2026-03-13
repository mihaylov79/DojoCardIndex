package cardindex.dojocardindex.exceptions;

public class AgreementNotFoundException extends RuntimeException{

    public AgreementNotFoundException(String message) {
        super(message);
    }

    public AgreementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
