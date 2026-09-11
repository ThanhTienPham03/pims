package saigonbank.fit.ims.exception;

public class InvalidImportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidImportException(String message) {
        super(message);
    }
}
