package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.request.ImportRequest;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.service.ImportService;

/**
 * ImportProcessDelegate — Xử lý các bước tự động trong Quy trình Nhập Ấn Phẩm (BPMN).
 * 
 * Task_KiemTraVaGhiNhanNhap: Thẩm định khóa tháng, quy tắc ACQT, cấp số phiếu và ghi nhận kho.
 */
@Component("importProcessDelegate")
public class ImportProcessDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ImportProcessDelegate.class);

    @Autowired(required = false)
    private ImportService importService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("[Camunda BPMN - Nhập Ấn Phẩm] Thực thi bước: {} | Process Instance ID: {}", activityId, processInstanceId);

        Object reqObj = execution.getVariable("importRequest");
        String username = (String) execution.getVariable("username");
        if (username == null) username = "camunda_system";

        if (reqObj instanceof ImportRequest importRequest && importService != null) {
            ImportResponse resp = importService.createImport(importRequest, username);
            execution.setVariable("soPhieu", resp.getSoPhieu());
            execution.setVariable("tongTien", resp.getTongTien());
            log.info("[Task_KiemTraVaGhiNhanNhap] Đã ghi nhận phiếu nhập thành công: {}", resp.getSoPhieu());
        } else {
            // Trường hợp phiếu đã được tạo sẵn từ Controller hoặc chạy mô phỏng
            String existingSoPhieu = (String) execution.getVariable("soPhieu");
            if (existingSoPhieu == null) {
                existingSoPhieu = "PN-SIM-" + System.currentTimeMillis();
                execution.setVariable("soPhieu", existingSoPhieu);
            }
            log.info("[Task_KiemTraVaGhiNhanNhap] Phiếu {} sẵn sàng để Thủ kho kiểm đếm.", existingSoPhieu);
        }
    }
}
