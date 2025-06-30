package cardindex.dojocardindex.exceptions;

public class EventNotFoundException extends IllegalArgumentException{

    private final static String DEFAULT_MESSAGE = "Събитието не беше открито!";

    public EventNotFoundException(){
        super(DEFAULT_MESSAGE);
    }

    public EventNotFoundException(String message){
        super(message);
    }
}
