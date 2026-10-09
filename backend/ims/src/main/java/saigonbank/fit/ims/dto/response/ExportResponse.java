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
public class ExportResponse {
    private Long id;
    private String soPhieu;
    private LocalDate ngayXuat;
    private String noiNhan;
    private String lyDoXuat;
    private String tkNo;
    private String tkCo;
    private BigDecimal tongTien;
    private String trangThai;
    private String nhomDaiDien;
    private String workflowStatus;
    private String createdBy;
    private String creatorFullName;
    private LocalDateTime createdAt;
    private List<ExportDetailResponse> details;
}
