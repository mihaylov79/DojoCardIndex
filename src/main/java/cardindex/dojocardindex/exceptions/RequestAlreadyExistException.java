package cardindex.dojocardindex.exceptions;

public class RequestAlreadyExistException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Вече имате заявка за участие в това събитие!";

    public RequestAlreadyExistException(){

        super(DEFAULT_MESSAGE);
    }

    public RequestAlreadyExistException(String message){

        super(message);
    }
}
