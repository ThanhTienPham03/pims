package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String maSo;
    private String tenHang;
    private String nhomHang;
    private String nhomHangTen;
    private String donViTinh;
    private BigDecimal donGiaNhap;
    private String trangThai;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private boolean nameWarning;
    private String warningMessage;
}
