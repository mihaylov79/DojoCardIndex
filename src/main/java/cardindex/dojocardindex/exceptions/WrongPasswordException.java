package cardindex.dojocardindex.exceptions;

public class WrongPasswordException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Въведената парола е неправилна!";

    public WrongPasswordException(){

        super(DEFAULT_MESSAGE);
    }

    public WrongPasswordException(String message){

        super(message);
    }
}
