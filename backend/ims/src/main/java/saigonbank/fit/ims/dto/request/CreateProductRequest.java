package saigonbank.fit.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    // Mã số có thể để trống, hệ thống sẽ tự phát sinh nếu không truyền
    private String maSo;

    @NotBlank(message = "Tên hàng hóa là bắt buộc")
    private String tenHang;

    @NotBlank(message = "Nhóm hàng là bắt buộc")
    private String nhomHang; // VPP, ACTT, ACQT, VLK

    @NotBlank(message = "Đơn vị tính là bắt buộc")
    private String donViTinh;

    @NotNull(message = "Đơn giá nhập là bắt buộc")
    @PositiveOrZero(message = "Đơn giá nhập phải >= 0")
    private BigDecimal donGiaNhap;

    private Boolean forceSaveIfDuplicateName = false;
}
