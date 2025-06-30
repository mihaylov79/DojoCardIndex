package cardindex.dojocardindex.exceptions;

public class IllegalEventOperationException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Действието което се опитвате да извършите не е позволено.";

    public IllegalEventOperationException() {
        super(DEFAULT_MESSAGE);
    }

    public IllegalEventOperationException(String message) {
        super(message);
    }
}
