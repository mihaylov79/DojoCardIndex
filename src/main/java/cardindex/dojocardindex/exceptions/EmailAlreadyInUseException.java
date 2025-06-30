package cardindex.dojocardindex.exceptions;

public class EmailAlreadyInUseException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Потребител с такава електронна поща вече съществува.";

    public EmailAlreadyInUseException() {
        super(DEFAULT_MESSAGE);
    }

    public EmailAlreadyInUseException(String message){

        super(message);
    }
}
