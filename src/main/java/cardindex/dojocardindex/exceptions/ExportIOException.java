package cardindex.dojocardindex.exceptions;

public class ExportIOException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Грешка при експортирането на посоченият ресурс.";

    public ExportIOException() {
        super(DEFAULT_MESSAGE);
    }

    public ExportIOException(String message) {
        super(message);
    }
}
