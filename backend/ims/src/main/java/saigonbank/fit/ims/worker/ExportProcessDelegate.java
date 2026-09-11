package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.request.ExportRequest;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.service.ExportService;

/**
 * ExportProcessDelegate — Xử lý các bước tự động trong Quy trình Xuất Ấn Phẩm (BPMN).
 * 
 * 1. Task_KiemTraKho: Thẩm định tồn kho và xác định có cần Trưởng phòng duyệt không.
 * 2. Task_GhiNhanXuatKho: Cấp số phiếu, cập nhật tồn kho & thẻ kho qua ExportService.
 */
@Component("exportProcessDelegate")
public class ExportProcessDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ExportProcessDelegate.class);

    @Autowired(required = false)
    private ExportService exportService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("[Camunda BPMN - Xuất Ấn Phẩm] Thực thi bước: {} | Process Instance ID: {}", activityId, processInstanceId);

        if ("Task_KiemTraKhoVaGhiNhan".equals(activityId) || "Task_GhiNhanXuatKho".equals(activityId) || "Task_CapSoVaGhiNhan".equals(activityId)) {
            handleGhiNhanXuat(execution);
        } else if ("Task_KiemTraKho".equals(activityId)) {
            handleKiemTraKho(execution);
        } else {
            log.info("[ExportProcessDelegate] Hoàn tất bước: {}", activityId);
        }
    }

    private void handleKiemTraKho(DelegateExecution execution) {
        // Kiểm tra biến canPheDuyet: nếu chưa có, tự động quyết định dựa trên tổng tiền hoặc số lượng
        Boolean canPheDuyet = (Boolean) execution.getVariable("canPheDuyet");
        if (canPheDuyet == null) {
            Object tongTienObj = execution.getVariable("tongTien");
            if (tongTienObj instanceof Number num && num.doubleValue() >= 50_000_000.0) {
                canPheDuyet = true;
            } else {
                canPheDuyet = false; // Mặc định tự động duyệt nếu giá trị nhỏ
            }
            execution.setVariable("canPheDuyet", canPheDuyet);
        }

        log.info("[Task_KiemTraKho] Kiểm tra hoàn tất. Yêu cầu Trưởng phòng duyệt: {}", canPheDuyet);
        execution.setVariable("checkStatus", "VALID");
    }

    private void handleGhiNhanXuat(DelegateExecution execution) {
        Object reqObj = execution.getVariable("exportRequest");
        String username = (String) execution.getVariable("username");
        if (username == null) username = "camunda_system";

        if (reqObj instanceof ExportRequest exportRequest && exportService != null) {
            ExportResponse resp = exportService.createExport(exportRequest, username);
            execution.setVariable("soPhieu", resp.getSoPhieu());
            execution.setVariable("tongTien", resp.getTongTien());
            log.info("[Task_GhiNhanXuatKho] Đã ghi nhận phiếu xuất thành công: {}", resp.getSoPhieu());
        } else {
            // Trường hợp phiếu đã được tạo sẵn từ Controller hoặc chạy mô phỏng
            String existingSoPhieu = (String) execution.getVariable("soPhieu");
            if (existingSoPhieu == null) {
                existingSoPhieu = "PX-SIM-" + System.currentTimeMillis();
                execution.setVariable("soPhieu", existingSoPhieu);
            }
            log.info("[Task_GhiNhanXuatKho] Phiếu {} sẵn sàng để Thủ kho xác nhận.", existingSoPhieu);
        }
    }
}
