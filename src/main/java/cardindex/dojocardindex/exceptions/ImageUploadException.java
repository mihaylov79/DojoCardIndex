package cardindex.dojocardindex.exceptions;

/**
 * Хвърля се при грешка при качване на изображение в Cloudinary.
 */
public class ImageUploadException extends RuntimeException {

    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
