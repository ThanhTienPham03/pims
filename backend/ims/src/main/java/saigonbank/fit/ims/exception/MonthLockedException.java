package saigonbank.fit.ims.exception;

public class MonthLockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MonthLockedException(String message) {
        super(message);
    }

    public MonthLockedException(int month, int year) {
        super("Tháng " + month + "/" + year + " đã bị khóa. Không thể thực hiện thao tác này.");
    }
}
