package cardindex.dojocardindex.exceptions;

public class PasswordDoesNotMatchException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Въведените пароли не съвпадат!";

    public PasswordDoesNotMatchException(){

        super(DEFAULT_MESSAGE);
    }

    public PasswordDoesNotMatchException(String message){

        super(message);
    }
}
