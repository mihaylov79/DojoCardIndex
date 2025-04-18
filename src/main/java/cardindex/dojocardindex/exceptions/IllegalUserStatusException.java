package cardindex.dojocardindex.exceptions;

public class IllegalUserStatusException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Този потребител е деактивиран , няма регистрация или чака потвърждение от администратор!";

    public IllegalUserStatusException() {
        super(DEFAULT_MESSAGE);
    }

    public IllegalUserStatusException(String message) {
        super(message);
    }



}
