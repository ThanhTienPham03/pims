package saigonbank.fit.ims.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import saigonbank.fit.ims.dto.response.ExportApprovalDecisionResult;
import saigonbank.fit.ims.service.CamundaDmnService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CamundaDmnTest {

    @Autowired
    private CamundaDmnService camundaDmnService;

    @Test
    @DisplayName("DMN Rule 1: Hàng ACQT (Ấn chỉ quan trọng) -> Bắt buộc Ban Giám Đốc duyệt kể cả giá trị nhỏ")
    void testRule1_ACQT_AlwaysRequiresDirectorApproval() {
        ExportApprovalDecisionResult result = camundaDmnService.evaluateExportApproval(
                "ACQT",
                new BigDecimal("500000"), // 500k VNĐ
                "Xuất phôi sổ tiết kiệm cho PGD"
        );

        assertThat(result).isNotNull();
        assertThat(result.getCanPheDuyet()).as("Hàng ACQT bắt buộc phải duyệt").isTrue();
        assertThat(result.getCapPheDuyet()).isEqualTo("BAN_GIAM_DOC");
        assertThat(result.getHanDuyetGio()).isEqualTo(24);
        assertThat(result.getMoTaLyDo()).contains("kiểm soát đặc biệt");
    }

    @Test
    @DisplayName("DMN Rule 2: Giá trị xuất lớn >= 50 triệu VNĐ -> Ban Giám Đốc phê duyệt")
    void testRule2_LargeAmount_RequiresDirectorApproval() {
        ExportApprovalDecisionResult result = camundaDmnService.evaluateExportApproval(
                "VPP",
                new BigDecimal("65000000"), // 65 triệu VNĐ
                "Xuất văn phòng phẩm đợt 1 toàn ngân hàng"
        );

        assertThat(result).isNotNull();
        assertThat(result.getCanPheDuyet()).isTrue();
        assertThat(result.getCapPheDuyet()).isEqualTo("BAN_GIAM_DOC");
        assertThat(result.getHanDuyetGio()).isEqualTo(24);
        assertThat(result.getMoTaLyDo()).contains("50 triệu VNĐ trở lên");
    }

    @Test
    @DisplayName("DMN Rule 2 (Biên): Giá trị xuất đúng bằng 50 triệu VNĐ -> Ban Giám Đốc phê duyệt")
    void testRule2_BoundaryExact50Million() {
        ExportApprovalDecisionResult result = camundaDmnService.evaluateExportApproval(
                "VLK",
                new BigDecimal("50000000"),
                "Xuất vật liệu kim khí"
        );

        assertThat(result.getCanPheDuyet()).isTrue();
        assertThat(result.getCapPheDuyet()).isEqualTo("BAN_GIAM_DOC");
        assertThat(result.getHanDuyetGio()).isEqualTo(24);
    }

    @Test
    @DisplayName("DMN Rule 3: Giá trị xuất trung bình từ 10 đến < 50 triệu VNĐ -> Trưởng phòng phê duyệt")
    void testRule3_MediumAmount_RequiresDepartmentHeadApproval() {
        ExportApprovalDecisionResult result = camundaDmnService.evaluateExportApproval(
                "VPP",
                new BigDecimal("25000000"), // 25 triệu VNĐ
                "Xuất vật tư chi nhánh"
        );

        assertThat(result).isNotNull();
        assertThat(result.getCanPheDuyet()).isTrue();
        assertThat(result.getCapPheDuyet()).isEqualTo("TRUONG_PHONG");
        assertThat(result.getHanDuyetGio()).isEqualTo(12);
        assertThat(result.getMoTaLyDo()).contains("Trưởng phòng phê duyệt");
    }

    @Test
    @DisplayName("DMN Rule 3 (Biên): Giá trị xuất đúng bằng 10 triệu VNĐ -> Trưởng phòng phê duyệt")
    void testRule3_BoundaryExact10Million() {
        ExportApprovalDecisionResult result = camundaDmnService.evaluateExportApproval(
                "ACTT",
                new BigDecimal("10000000"),
                "Xuất ấn chỉ thông thường"
        );

        assertThat(result.getCanPheDuyet()).isTrue();
        assertThat(result.getCapPheDuyet()).isEqualTo("TRUONG_PHONG");
        assertThat(result.getHanDuyetGio()).isEqualTo(12);
    }

    @Test
    @DisplayName("DMN Rule 4: Giá trị xuất nhỏ < 10 triệu VNĐ -> Hệ thống tự động duyệt, không cần cấp duyệt")
    void testRule4_SmallAmount_AutoApproved() {
        ExportApprovalDecisionResult result = camundaDmnService.evaluateExportApproval(
                "VPP",
                new BigDecimal("3500000"), // 3.5 triệu VNĐ
                "Xuất giấy in phòng Kế toán"
        );

        assertThat(result).isNotNull();
        assertThat(result.getCanPheDuyet()).as("Dưới 10 triệu cho VPP không cần duyệt").isFalse();
        assertThat(result.getCapPheDuyet()).isEqualTo("TU_DONG");
        assertThat(result.getHanDuyetGio()).isEqualTo(0);
        assertThat(result.getMoTaLyDo()).contains("tự động duyệt xuất kho");
    }
}
