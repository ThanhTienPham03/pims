package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.request.ImportRequest;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.repository.InventoryTransactionRepository;
import saigonbank.fit.ims.service.ImportService;

@Component("importProcessDelegate")
public class ImportProcessDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ImportProcessDelegate.class);

    public static final ThreadLocal<ImportResponse> currentImportResponse = new ThreadLocal<>();

    @Autowired(required = false)
    private ImportService importService;

    @Autowired(required = false)
    private InventoryTransactionRepository transactionRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("[Camunda BPMN - Nhập Ấn Phẩm] Thực thi bước: {} | Process Instance ID: {}", activityId,
                processInstanceId);

        Object reqObj = execution.getVariable("importRequest");
        String username = (String) execution.getVariable("username");
        if (username == null) {
            username = (String) execution.getVariable("fullname");
        }
        if (username == null) {
            username = "camunda_system";
        }

        if (reqObj instanceof ImportRequest importRequest && importService != null) {
            ImportResponse resp = importService.createImport(importRequest, username);
            currentImportResponse.set(resp);
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
                     [CAMUNDA IMPORT RESULT - KẾT QUẢ QUY TRÌNH NHẬP ẤN PHẨM]
                    ▶ Bước thực thi         : {}
                    ▶ Process Instance ID   : {}
                    ▶ Số phiếu nhập         : {}
                    ▶ Tổng tiền             : {}
                    ▶ Người thực hiện       : {}
                    ▶ Kết quả               : Đã ghi nhận thành công vào kho
                    ================================================================================
                    """, activityId, processInstanceId, resp.getSoPhieu(), resp.getTongTien(), username);
        } else {
            // Trường hợp phiếu đã được tạo sẵn từ Controller hoặc chạy mô phỏng
            String existingSoPhieu = (String) execution.getVariable("soPhieu");
            if (existingSoPhieu == null) {
                existingSoPhieu = "PN-SIM-" + System.currentTimeMillis();
                execution.setVariable("soPhieu", existingSoPhieu);
            }
            log.info("""
                    \n================================================================================
                     [CAMUNDA IMPORT RESULT - KẾT QUẢ BƯỚC NHẬP ẤN PHẨM]
                    ▶ Bước thực thi         : {}
                    ▶ Process Instance ID   : {}
                    ▶ Số phiếu              : {}
                    ▶ Người thực hiện       : {}
                    ▶ Kết quả               : Phiếu sẵn sàng để Thủ kho kiểm đếm
                    ================================================================================
                    """, activityId, processInstanceId, existingSoPhieu, username);
        }
    }
}
