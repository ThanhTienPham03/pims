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
public class ImportDetailResponse {
    private Long id;
    private Long productId;
    private String maSo;
    private String tenHang;
    private String nhomHang;
    private String donViTinh;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
