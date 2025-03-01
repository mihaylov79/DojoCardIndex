package cardindex.dojocardindex.exceptions;

import lombok.Builder;

public class MessageNotFoundException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Съобщението не беше намерено.";

    public MessageNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public MessageNotFoundException(String message) {
        super(message);
    }


}
