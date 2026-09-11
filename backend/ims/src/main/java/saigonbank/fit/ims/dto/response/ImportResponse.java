package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private Long id;
    private String soPhieu;
    private LocalDate ngayNhap;
    private String noiNhap;
    private String noiMua;
    private String soHoaDon;
    private LocalDate ngayHoaDon;
    private String tkNo;
    private String tkCo;
    private BigDecimal tongTien;
    private String trangThai;
    private String nhomDaiDien;
    private String workflowStatus;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<ImportDetailResponse> details;
}
