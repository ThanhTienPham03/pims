package saigonbank.fit.ims.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.DecisionService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import saigonbank.fit.ims.dto.response.ExportApprovalDecisionResult;

import java.math.BigDecimal;
import java.util.Map;

/**
 * CamundaDmnService — Dịch vụ thực thi và đánh giá các bảng quyết định DMN (Decision Model and Notation).
 * 
 * Sử dụng Camunda 7 DecisionService và FEEL Engine để thẩm định tự động các quy tắc nghiệp vụ.
 */
@Slf4j
@Service
public class CamundaDmnService {

    public static final String DECISION_PHE_DUYET_XUAT = "Decision_PheDuyetXuat";

    @Autowired(required = false)
    private DecisionService decisionService;

    /**
     * Đánh giá quy tắc phê duyệt xuất ấn phẩm thông qua bảng DMN Decision_PheDuyetXuat.
     *
     * @param nhomHang  Nhóm hàng: ACQT | VPP | ACTT | VLK
     * @param tongTien  Tổng tiền của phiếu xuất (VNĐ)
     * @param lyDoXuat  Lý do xuất (tuỳ chọn)
     * @return Kết quả thẩm định DMN
     */
    public ExportApprovalDecisionResult evaluateExportApproval(String nhomHang, BigDecimal tongTien, String lyDoXuat) {
        String group = nhomHang != null ? nhomHang.trim().toUpperCase() : "VPP";
        BigDecimal amount = tongTien != null ? tongTien : BigDecimal.ZERO;
        double amountDouble = amount.doubleValue();

        if (decisionService == null) {
            log.warn("[Camunda DMN] DecisionService không khả dụng, sử dụng luật dự phòng mặc định.");
            boolean defaultApprove = "ACQT".equals(group) || amountDouble >= 50_000_000.0;
            return ExportApprovalDecisionResult.builder()
                    .nhomHang(group)
                    .tongTien(amount)
                    .canPheDuyet(defaultApprove)
                    .capPheDuyet(defaultApprove ? "TRUONG_PHONG" : "TU_DONG")
                    .hanDuyetGio(defaultApprove ? 24 : 0)
                    .moTaLyDo("Luật dự phòng (Không kết nối DMN Engine)")
                    .decisionKey(DECISION_PHE_DUYET_XUAT)
                    .build();
        }

        try {
            VariableMap variables = Variables.createVariables()
                    .putValue("nhomHang", group)
                    .putValue("tongTien", amountDouble);

            DmnDecisionTableResult tableResult = decisionService.evaluateDecisionTableByKey(
                    DECISION_PHE_DUYET_XUAT,
                    variables
            );

            Map<String, Object> firstRule = tableResult.getFirstResult();
            if (firstRule == null || firstRule.isEmpty()) {
                log.warn("[Camunda DMN] Không có quy tắc nào khớp trong bảng {} cho nhomHang={}, tongTien={}",
                        DECISION_PHE_DUYET_XUAT, group, amount);
                return fallbackResult(group, amount);
            }

            Boolean canPheDuyet = (Boolean) firstRule.get("canPheDuyet");
            String capPheDuyet = (String) firstRule.get("capPheDuyet");
            Number hanDuyetNumber = (Number) firstRule.get("hanDuyetGio");
            Integer hanDuyetGio = hanDuyetNumber != null ? hanDuyetNumber.intValue() : 0;
            String moTaLyDo = (String) firstRule.get("moTaLyDo");

            ExportApprovalDecisionResult result = ExportApprovalDecisionResult.builder()
                    .nhomHang(group)
                    .tongTien(amount)
                    .canPheDuyet(canPheDuyet != null && canPheDuyet)
                    .capPheDuyet(capPheDuyet != null ? capPheDuyet : "TU_DONG")
                    .hanDuyetGio(hanDuyetGio)
                    .moTaLyDo(moTaLyDo != null ? moTaLyDo : "Thực thi thành công từ DMN")
                    .decisionKey(DECISION_PHE_DUYET_XUAT)
                    .build();

            // Ghi log kết quả quyết định DMN ra Terminal Backend rõ ràng
            log.info("""
                \n================================================================================
                🧠 [CAMUNDA DMN DECISION EVALUATED - THẨM ĐỊNH BẢNG QUYẾT ĐỊNH]
                ▶ Quyết định (Decision) : {}
                ▶ Nhóm hàng đầu vào     : {}
                ▶ Tổng tiền đầu vào     : {} VNĐ
                ▶ Cần phê duyệt?        : {}
                ▶ Cấp phê duyệt         : {}
                ▶ Thời hạn duyệt        : {} giờ
                ▶ Căn cứ quy định       : {}
                ================================================================================
                """,
                DECISION_PHE_DUYET_XUAT,
                group,
                String.format("%,.0f", amountDouble),
                result.getCanPheDuyet() ? "CÓ (YÊU CẦU DUYỆT)" : "KHÔNG (TỰ ĐỘNG DUYỆT)",
                result.getCapPheDuyet(),
                result.getHanDuyetGio(),
                result.getMoTaLyDo()
            );

            return result;
        } catch (Exception e) {
            log.error("[Camunda DMN] Lỗi khi đánh giá bảng {}: {}", DECISION_PHE_DUYET_XUAT, e.getMessage(), e);
            return fallbackResult(group, amount);
        }
    }

    private ExportApprovalDecisionResult fallbackResult(String group, BigDecimal amount) {
        boolean need = "ACQT".equals(group) || amount.doubleValue() >= 50_000_000.0;
        return ExportApprovalDecisionResult.builder()
                .nhomHang(group)
                .tongTien(amount)
                .canPheDuyet(need)
                .capPheDuyet(need ? ("ACQT".equals(group) ? "BAN_GIAM_DOC" : "TRUONG_PHONG") : "TU_DONG")
                .hanDuyetGio(need ? 24 : 0)
                .moTaLyDo("Áp dụng quy tắc mặc định an toàn")
                .decisionKey(DECISION_PHE_DUYET_XUAT)
                .build();
    }
}
