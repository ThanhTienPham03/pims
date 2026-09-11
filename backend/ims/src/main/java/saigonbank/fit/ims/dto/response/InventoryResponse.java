package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long inventoryId;
    private Long productId;
    private String maSo;
    private String tenHang;
    private String nhomHang;
    private String nhomHangTen;
    private String donViTinh;
    private String trangThai;

    // Tồn đầu kỳ
    private BigDecimal openingQuantity;
    private BigDecimal openingAmount;

    // Nhập trong kỳ
    private BigDecimal importQuantity;
    private BigDecimal importAmount;

    // Xuất trong kỳ
    private BigDecimal exportQuantity;
    private BigDecimal exportAmount;

    // Tồn cuối kỳ
    private BigDecimal endingQuantity;
    private BigDecimal endingAmount;

    // Giá bình quân
    private BigDecimal averagePrice;
}
