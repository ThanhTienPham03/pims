package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.request.ExportRequest;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.repository.InventoryTransactionRepository;
import saigonbank.fit.ims.service.CamundaDmnService;
import saigonbank.fit.ims.service.ExportService;

import java.math.BigDecimal;

/**
 * ExportProcessDelegate — Xử lý các bước tự động trong Quy trình Xuất Ấn Phẩm (BPMN).
 * 
 * 1. Task_KiemTraKho: Thẩm định tồn kho và quyết định phê duyệt bằng bảng quyết định DMN (Decision_PheDuyetXuat).
 * 2. Task_GhiNhanXuatKho: Cấp số phiếu, cập nhật tồn kho & thẻ kho qua ExportService.
 */
@Component("exportProcessDelegate")
public class ExportProcessDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ExportProcessDelegate.class);

    public static final ThreadLocal<ExportResponse> currentExportResponse = new ThreadLocal<>();

    @Autowired(required = false)
    private ExportService exportService;

    @Autowired(required = false)
    private CamundaDmnService camundaDmnService;

    @Autowired(required = false)
    private InventoryTransactionRepository transactionRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("[Camunda BPMN - Xuất Ấn Phẩm] Thực thi bước: {} | Process Instance ID: {}", activityId, processInstanceId);

        if ("Task_CapSoVaGhiNhan".equals(activityId)) {
            handleGhiNhanXuat(execution);
        } else if ("Task_KiemTraKho".equals(activityId)) {
            handleKiemTraKho(execution);
        } else {
            log.info("[ExportProcessDelegate] Hoàn tất bước: {}", activityId);
        }
    }

    private void handleKiemTraKho(DelegateExecution execution) {
        String nhomHang = (String) execution.getVariable("nhomHang");
        Object tongTienObj = execution.getVariable("tongTien");
        BigDecimal tongTien = BigDecimal.ZERO;
        if (tongTienObj instanceof Number num) {
            tongTien = BigDecimal.valueOf(num.doubleValue());
        }

        if (camundaDmnService != null) {
            var decision = camundaDmnService.evaluateExportApproval(nhomHang, tongTien, (String) execution.getVariable("lyDoXuat"));
            execution.setVariable("canPheDuyet", decision.getCanPheDuyet());
            execution.setVariable("capPheDuyet", decision.getCapPheDuyet());
            execution.setVariable("hanDuyetGio", decision.getHanDuyetGio());
            execution.setVariable("moTaLyDo", decision.getMoTaLyDo());
            log.info("[Task_KiemTraKho] Thẩm định DMN thành công: Cần duyệt={}, Cấp duyệt={}, Lý do='{}'",
                    decision.getCanPheDuyet(), decision.getCapPheDuyet(), decision.getMoTaLyDo());
        } else {
            boolean canPheDuyet = "ACQT".equalsIgnoreCase(nhomHang) || tongTien.doubleValue() >= 50_000_000.0;
            execution.setVariable("canPheDuyet", canPheDuyet);
            log.info("[Task_KiemTraKho] Kiểm tra hoàn tất (fallback). Yêu cầu duyệt: {}", canPheDuyet);
        }

        execution.setVariable("checkStatus", "VALID");
    }

    private void handleGhiNhanXuat(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();
        Object reqObj = execution.getVariable("exportRequest");
        String username = (String) execution.getVariable("username");
        if (username == null) username = "camunda_system";

        if (reqObj instanceof ExportRequest exportRequest && exportService != null) {
            ExportResponse resp = exportService.createExport(exportRequest, username);
            currentExportResponse.set(resp);
            execution.setVariable("soPhieu", resp.getSoPhieu());
            execution.setVariable("tongTien", resp.getTongTien());

            if (transactionRepository != null && resp.getId() != null) {
                transactionRepository.findById(resp.getId()).ifPresent(tx -> {
                    tx.setWorkflowKey(processInstanceId);
                    tx.setWorkflowStatus("COMPLETED");
                    transactionRepository.save(tx);
                });
            }

            log.info("""
                \n================================================================================
                📤 [CAMUNDA EXPORT RESULT - KẾT QUẢ QUY TRÌNH XUẤT ẤN PHẨM]
                ▶ Bước thực thi         : {}
                ▶ Process Instance ID   : {}
                ▶ Số phiếu xuất         : {}
                ▶ Tổng tiền             : {}
                ▶ Người thực hiện       : {}
                ▶ Kết quả               : Đã ghi nhận phiếu xuất thành công
                ================================================================================
                """, activityId, processInstanceId, resp.getSoPhieu(), resp.getTongTien(), username);
        } else {
            // Trường hợp phiếu đã được tạo sẵn từ Controller hoặc chạy mô phỏng
            String existingSoPhieu = (String) execution.getVariable("soPhieu");
            if (existingSoPhieu == null) {
                existingSoPhieu = "PX-SIM-" + System.currentTimeMillis();
                execution.setVariable("soPhieu", existingSoPhieu);
            }
            log.info("""
                \n================================================================================
                📤 [CAMUNDA EXPORT RESULT - KẾT QUẢ BƯỚC XUẤT ẤN PHẨM]
                ▶ Bước thực thi         : {}
                ▶ Process Instance ID   : {}
                ▶ Số phiếu              : {}
                ▶ Người thực hiện       : {}
                ▶ Kết quả               : Phiếu sẵn sàng để Thủ kho xác nhận
                ================================================================================
                """, activityId, processInstanceId, existingSoPhieu, username);
        }
    }
}
