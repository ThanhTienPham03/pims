package saigonbank.fit.ims.exception;

public class InvalidExportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidExportException(String message) {
        super(message);
    }
}
