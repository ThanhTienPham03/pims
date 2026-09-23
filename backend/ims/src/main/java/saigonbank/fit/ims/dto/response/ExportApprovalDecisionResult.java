package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kết quả thẩm định từ Bảng quyết định Camunda DMN (Decision_PheDuyetXuat).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportApprovalDecisionResult {

    /**
     * Nhóm hàng đầu vào (VPP, ACQT, ACTT, VLK)
     */
    private String nhomHang;

    /**
     * Tổng giá trị xuất kho đầu vào (VNĐ)
     */
    private BigDecimal tongTien;

    /**
     * Có yêu cầu phê duyệt hay không (true/false)
     */
    private Boolean canPheDuyet;

    /**
     * Cấp phê duyệt: TU_DONG | TRUONG_PHONG | BAN_GIAM_DOC
     */
    private String capPheDuyet;

    /**
     * Thời hạn phê duyệt quy định (giờ): 0 | 12 | 24
     */
    private Integer hanDuyetGio;

    /**
     * Căn cứ quy chế và mô tả chi tiết của quyết định
     */
    private String moTaLyDo;

    /**
     * Tên quyết định DMN đã đánh giá
     */
    private String decisionKey;
}
