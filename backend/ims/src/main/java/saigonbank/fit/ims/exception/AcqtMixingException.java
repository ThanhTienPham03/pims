package saigonbank.fit.ims.exception;

public class AcqtMixingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AcqtMixingException(String message) {
        super(message);
    }

    public AcqtMixingException() {
        super("ACQT phải lập phiếu xuất riêng, không được gộp chung với nhóm hàng khác.");
    }
}
