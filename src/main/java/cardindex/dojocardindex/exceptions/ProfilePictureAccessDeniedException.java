package cardindex.dojocardindex.exceptions;

public class ProfilePictureAccessDeniedException extends RuntimeException {

    public ProfilePictureAccessDeniedException(String message) {
        super(message);
    }

    public ProfilePictureAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
