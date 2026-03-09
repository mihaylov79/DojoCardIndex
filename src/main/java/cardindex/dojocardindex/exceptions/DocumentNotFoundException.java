package cardindex.dojocardindex.exceptions;

import jakarta.persistence.EntityNotFoundException;

public class DocumentNotFoundException extends EntityNotFoundException {

    public DocumentNotFoundException(String message) {
        super(message);
    }

    public DocumentNotFoundException(Exception cause) {
        super(cause);
    }
}
