package saigonbank.fit.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {

    @NotBlank(message = "Tên hàng hóa là bắt buộc")
    private String tenHang;

    @NotBlank(message = "Đơn vị tính là bắt buộc")
    private String donViTinh;

    @PositiveOrZero(message = "Đơn giá nhập phải >= 0")
    private BigDecimal donGiaNhap;

    /** HOAT_DONG | NGUNG_SU_DUNG */
    private String trangThai;
}
