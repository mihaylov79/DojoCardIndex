package cardindex.dojocardindex.exceptions;



public class MessageNotFoundException extends IllegalArgumentException{

    private static final String DEFAULT_MESSAGE = "Съобщението не беше намерено.";

    public MessageNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public MessageNotFoundException(String message) {
        super(message);
    }


}
