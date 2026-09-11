package saigonbank.fit.ims.worker;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("demoTask")
public class CamundaWorker implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CamundaWorker.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Object message = execution.getVariable("message");

        log.info("============== CAMUNDA 7 DELEGATE ĐÃ NHẬN TASK ==============");
        log.info("Process Instance ID: {}", execution.getProcessInstanceId());
        log.info("Business Key: {}", execution.getBusinessKey());
        log.info("Message nhận được: {}", message);
        log.info("==============================================================");

        execution.setVariable("result", "Đã xử lý thành công thông báo: " + message);
    }
}
