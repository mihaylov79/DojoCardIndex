package cardindex.dojocardindex.exceptions;

/**
 * Хвърля се при грешка при изтриване на изображение от Cloudinary.
 */
public class ImageDeleteException extends RuntimeException {

    public ImageDeleteException(String message) {
        super(message);
    }

    public ImageDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
