package saigonbank.fit.ims.exception;

public class ProductDeletionNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProductDeletionNotAllowedException(String message) {
        super(message);
    }
}
