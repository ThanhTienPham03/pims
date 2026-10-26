package saigonbank.fit.ims.listener;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaErrorLoggingListener {

    @EventListener
    public void onExecutionEvent(DelegateExecution execution) {
        String eventName = execution.getEventName();
        FlowElement flowElement = execution.getBpmnModelElementInstance();
        if (flowElement == null) {
            return;
        }

        // 1. Trường hợp: Quy trình bắt đầu (StartEvent)
        if ("start".equals(eventName) && flowElement instanceof StartEvent startEvent
                && execution.getActivityInstanceId() != null
                && execution.getActivityInstanceId().startsWith(startEvent.getId())) {
            String activityName = startEvent.getName() != null ? startEvent.getName() : startEvent.getId();
            log.info("""
                    \n================================================================================
                    [CAMUNDA WORKFLOW STARTED - BẮT ĐẦU QUY TRÌNH]
                    ▶ Quy trình (Process)   : {}
                    ▶ Mã tham chiếu (Key)   : {}
                    ▶ Điểm bắt đầu (Start)  : {} [{}]
                    ▶ Process Instance ID   : {}
                    ================================================================================
                    """,
                    execution.getProcessDefinitionId(),
                    execution.getProcessBusinessKey() != null ? execution.getProcessBusinessKey() : "N/A",
                    activityName,
                    startEvent.getId(),
                    execution.getProcessInstanceId());
            return;
        }

        // 2. Trường hợp: Bắt lỗi tại Boundary Error Event
        if ("start".equals(eventName) && flowElement instanceof BoundaryEvent boundaryEvent) {
            Collection<ErrorEventDefinition> errors = boundaryEvent.getChildElementsByType(ErrorEventDefinition.class);
            if (!errors.isEmpty()) {
                ErrorEventDefinition errDef = errors.iterator().next();
                String errorCode = errDef.getError() != null ? errDef.getError().getErrorCode() : "N/A";
                String errorName = errDef.getError() != null ? errDef.getError().getName() : "N/A";
                String activityName = boundaryEvent.getName() != null ? boundaryEvent.getName() : boundaryEvent.getId();

                log.error("""
                        \n================================================================================
                        [CAMUNDA BPMN ERROR CAUGHT - BẮT ĐƯỢC LỖI QUY TRÌNH]
                        ▶ Quy trình (Process)   : {}
                        ▶ Mã tham chiếu (Key)   : {}
                        ▶ Điểm bắt lỗi (Event)  : {} [{}]
                        ▶ Mã lỗi (Error Code)   : {}
                        ▶ Tên lỗi (Error Name)  : {}
                        ================================================================================
                        """,
                        execution.getProcessDefinitionId(),
                        execution.getProcessBusinessKey() != null ? execution.getProcessBusinessKey() : "N/A",
                        activityName,
                        boundaryEvent.getId(),
                        errorCode,
                        errorName);
            }
            return;
        }

        // 3. Trường hợp: Kết thúc quy trình tại EndEvent (Cả Thành công lẫn Lỗi)
        if ("start".equals(eventName) && flowElement instanceof EndEvent endEvent) {
            Collection<ErrorEventDefinition> errors = endEvent.getChildElementsByType(ErrorEventDefinition.class);
            String activityName = endEvent.getName() != null ? endEvent.getName() : endEvent.getId();

            if (!errors.isEmpty()) {
                // 3a. Kết thúc với lỗi (Error End Event)
                ErrorEventDefinition errDef = errors.iterator().next();
                String errorCode = errDef.getError() != null ? errDef.getError().getErrorCode() : "N/A";

                log.warn("""
                        \n================================================================================
                        [CAMUNDA WORKFLOW REJECTED - TIẾN TRÌNH KẾT THÚC VÌ LỖI]
                        ▶ Quy trình (Process)   : {}
                        ▶ Mã tham chiếu (Key)   : {}
                        ▶ Điểm kết thúc (End)   : {} [{}]
                        ▶ Mã lỗi (Error Code)   : {}
                        ▶ Kết quả               : BỊ CHẶN DỪNG (REJECTED / TERMINATED)
                        ================================================================================
                        """,
                        execution.getProcessDefinitionId(),
                        execution.getProcessBusinessKey() != null ? execution.getProcessBusinessKey() : "N/A",
                        activityName,
                        endEvent.getId(),
                        errorCode);
            } else {
                // 3b. Kết thúc thành công (None End Event / Success End Event)
                Map<String, Object> vars = execution.getVariables();
                log.info("""
                        \n================================================================================
                        [CAMUNDA WORKFLOW RESULT - KẾT QUẢ QUY TRÌNH THÀNH CÔNG]
                        ▶ Quy trình (Process)   : {}
                        ▶ Mã tham chiếu (Key)   : {}
                        ▶ Điểm kết thúc (End)   : {} [{}]
                        ▶ Process Instance ID   : {}
                        ▶ Kết quả               : THỰC THI THÀNH CÔNG (COMPLETED)
                        ▶ Biến kết quả (Vars)   : {}
                        ================================================================================
                        """,
                        execution.getProcessDefinitionId(),
                        execution.getProcessBusinessKey() != null ? execution.getProcessBusinessKey() : "N/A",
                        activityName,
                        endEvent.getId(),
                        execution.getProcessInstanceId(),
                        formatVariablesForLog(vars));
            }
        }
    }

    /**
     * Lắng nghe sự kiện User Task (tạo mới và hoàn thành phê duyệt)
     */
    @EventListener
    public void onTaskEvent(DelegateTask task) {
        String eventName = task.getEventName();
        if ("create".equals(eventName)) {
            log.info("""
                    \n================================================================================
                    [CAMUNDA USER TASK ASSIGNED - CHỜ NGƯỜI DÙNG XỬ LÝ]
                    ▶ Tên Task              : {}
                    ▶ Task ID               : {}
                    ▶ Người được giao       : {}
                    ▶ Process Instance ID   : {}
                    ================================================================================
                    """,
                    task.getName(),
                    task.getId(),
                    task.getAssignee() != null ? task.getAssignee() : "Chưa chỉ định",
                    task.getProcessInstanceId());
        } else if ("complete".equals(eventName)) {
            log.info("""
                    \n================================================================================
                    [CAMUNDA USER TASK COMPLETED - ĐÃ HOÀN THÀNH TASK]
                    ▶ Tên Task              : {}
                    ▶ Task ID               : {}
                    ▶ Người thực hiện       : {}
                    ▶ Process Instance ID   : {}
                    ================================================================================
                    """,
                    task.getName(),
                    task.getId(),
                    task.getAssignee() != null ? task.getAssignee() : "N/A",
                    task.getProcessInstanceId());
        }
    }

    private String formatVariablesForLog(Map<String, Object> vars) {
        if (vars == null || vars.isEmpty()) {
            return "{}";
        }
        return vars.entrySet().stream()
                .filter(e -> e.getValue() != null && !(e.getValue() instanceof byte[]))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
