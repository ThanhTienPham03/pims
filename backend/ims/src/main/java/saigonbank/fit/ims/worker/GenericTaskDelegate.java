package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * GenericTaskDelegate — Delegate thực thi mặc định cho các bước tự động
 * trong các quy trình BPMN (như hiển thị thông tin, tính toán trung gian, kiểm tra hoặc ghi log audit).
 *
 * Hỗ trợ Camunda 7 engine giám sát và điều hướng quy trình trơn tru qua từng bước.
 */
@Component("genericTaskDelegate")
public class GenericTaskDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(GenericTaskDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String activityName = execution.getCurrentActivityName();
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("[BPMN Step] Đang thực thi bước: '{}' (ID: {}) trong Process Instance: {}",
                activityName != null ? activityName : "N/A",
                activityId,
                processInstanceId);
    }
}
