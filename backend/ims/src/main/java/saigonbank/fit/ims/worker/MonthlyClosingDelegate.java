package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.response.MonthlyClosingResponse;
import saigonbank.fit.ims.service.MonthlyClosingService;

import java.time.LocalDate;


@Component("monthlyClosingDelegate")
public class MonthlyClosingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(MonthlyClosingDelegate.class);

    @Autowired(required = false)
    private MonthlyClosingService monthlyClosingService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("[Camunda BPMN - Chốt Sổ] Đang thực hiện khóa sổ kỳ kế toán. Process Instance ID: {}",
                processInstanceId);

        Integer year = (Integer) execution.getVariable("year");
        Integer month = (Integer) execution.getVariable("month");
        String username = (String) execution.getVariable("username");

        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        if (username == null) {
            username = "camunda_system";
        }

        if (monthlyClosingService != null) {
            MonthlyClosingResponse resp = monthlyClosingService.lockMonth(year, month, username);
            execution.setVariable("closingResult", "SUCCESS");
            execution.setVariable("lockedAt", resp.getLockedAt() != null ? resp.getLockedAt().toString() : "");
            log.info("""
                    \n================================================================================
                    [CAMUNDA CLOSING RESULT - KẾT QUẢ QUY TRÌNH CHỐT SỔ]
                    ▶ Process Instance ID   : {}
                    ▶ Kỳ kế toán            : Tháng {}/{}
                    ▶ Người thực hiện       : {}
                    ▶ Thời điểm khóa        : {}
                    ▶ Trạng thái            : ĐÃ KHÓA SỔ & KẾT CHUYỂN THÀNH CÔNG
                    ================================================================================
                    """, processInstanceId, month, year, username, resp.getLockedAt());
        } else {
            log.info("""
                    \n================================================================================
                    [CAMUNDA CLOSING RESULT - KẾT QUẢ QUY TRÌNH CHỐT SỔ (MÔ PHỎNG)]
                    ▶ Process Instance ID   : {}
                    ▶ Kỳ kế toán            : Tháng {}/{}
                    ▶ Người thực hiện       : {}
                    ▶ Trạng thái            : Thành công
                    ================================================================================
                    """, processInstanceId, month, year, username);
        }
    }
}
