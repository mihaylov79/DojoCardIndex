package cardindex.dojocardindex.exceptions;

public class MessageCanNotBeSentToUserException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "Не може да бъде изпратено съобщение до този потребител.";

    public MessageCanNotBeSentToUserException() {

        super(DEFAULT_MESSAGE);
    }

    public MessageCanNotBeSentToUserException(String message){

        super(message);
    }
}
