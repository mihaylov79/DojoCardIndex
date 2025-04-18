package cardindex.dojocardindex.exceptions;

public class IllegalTokenException extends RuntimeException{

    private static final String DEFAILT_Message = "Токенът е изтекъл!";


    public IllegalTokenException() {

        super(DEFAILT_Message);
    }

    public IllegalTokenException(String message) {

        super(message);
    }


}
