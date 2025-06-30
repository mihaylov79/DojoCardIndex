package cardindex.dojocardindex.exceptions;

public class RequestNotFoundException extends IllegalArgumentException{

    private final static String DEFAULT_MESSAGE = "Заявката за регистрация не беше открита!";

    public RequestNotFoundException(){
        super(DEFAULT_MESSAGE);
    }

    public RequestNotFoundException(String message){
        super(message);
    }
}
