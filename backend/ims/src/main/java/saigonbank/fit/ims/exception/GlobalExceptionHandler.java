package saigonbank.fit.ims.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import saigonbank.fit.ims.dto.response.ErrorResponse;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProductAlreadyExists(ProductAlreadyExistsException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "PRODUCT_ALREADY_EXISTS", ex.getMessage(), request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK", ex.getMessage(), request);
    }

    @ExceptionHandler(AcqtMixingException.class)
    public ResponseEntity<ErrorResponse> handleAcqtMixing(AcqtMixingException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "ACQT_MIXING_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(MonthLockedException.class)
    public ResponseEntity<ErrorResponse> handleMonthLocked(MonthLockedException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "MONTH_LOCKED", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidImportException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImport(InvalidImportException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_IMPORT", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidExportException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExport(InvalidExportException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_EXPORT", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ProductDeletionNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleProductDeletionNotAllowed(ProductDeletionNotAllowedException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "PRODUCT_DELETION_NOT_ALLOWED", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không chính xác", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có quyền thực hiện thao tác này", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", errors, request);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        if (request.getRequestURI().endsWith("favicon.ico") || request.getRequestURI().endsWith(".ico")) {
            return ResponseEntity.notFound().build();
        }
        log.warn("Tài nguyên tĩnh không tồn tại: {}", request.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String msg = String.format("Tham số '%s' nhận giá trị '%s' không đúng kiểu dữ liệu yêu cầu (%s)",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "không xác định");
        log.warn("Lỗi sai kiểu tham số: {} trên {}", msg, request.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "METHOD_ARGUMENT_TYPE_MISMATCH", msg, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String code, String message, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
