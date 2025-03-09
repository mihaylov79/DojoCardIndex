package cardindex.dojocardindex.exceptions;

public class EventClosedException extends IllegalStateException{

    private final static String DEFAULT_MESSAGE = "Това събитие е затворено!";

    public EventClosedException(){
        super(DEFAULT_MESSAGE);
    }

    public EventClosedException(String message){
        super(message);
    }
}
