package saigonbank.fit.ims.controller;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camunda")
public class CamundaController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private saigonbank.fit.ims.service.CamundaWorkflowService camundaWorkflowService;

    @Autowired
    private saigonbank.fit.ims.service.CamundaDmnService camundaDmnService;

    @GetMapping("/processes")
    public ResponseEntity<?> listProcesses() {
        return ResponseEntity.ok(
                repositoryService.createProcessDefinitionQuery().list().stream()
                        .map(pd -> Map.of(
                                "id", pd.getId(),
                                "key", pd.getKey(),
                                "name", pd.getName() != null ? pd.getName() : pd.getKey(),
                                "version", pd.getVersion()))
                        .toList());
    }

    @PostMapping("/deploy")
    public ResponseEntity<String> deployProcesses() {
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name("IMS-Standard-Processes")
                    .addClasspathResource("demo-process.bpmn")
                    .addClasspathResource("bpmn/process_xuat_an_pham.bpmn")
                    .addClasspathResource("bpmn/process_nhap_an_pham.bpmn")
                    .addClasspathResource("bpmn/process_chot_so.bpmn")
                    .addClasspathResource("bpmn/process_quan_ly_danh_muc_an_pham.bpmn")
                    .addClasspathResource("dmn/decision_phe_duyet_xuat.dmn")
                    .enableDuplicateFiltering(true)
                    .deploy();

            return ResponseEntity.ok(
                    "Đã deploy thành công quy trình & bảng DMN lên Camunda 7! Deployment ID: " + deployment.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi deploy: " + e.getMessage());
        }
    }

    // ==========================================
    // 0. CAMUNDA DMN EVALUATION (BẢNG QUYẾT ĐỊNH)
    // ==========================================
    @PostMapping("/decision/export-approval")
    public ResponseEntity<?> evaluateExportApprovalDecision(
            @RequestParam(defaultValue = "VPP") String nhomHang,
            @RequestParam(defaultValue = "15000000") java.math.BigDecimal tongTien,
            @RequestParam(required = false, defaultValue = "Xuất sử dụng") String lyDoXuat) {
        var result = camundaDmnService.evaluateExportApproval(nhomHang, tongTien, lyDoXuat);
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // 1. QUY TRÌNH XUẤT ẤN PHẨM (APPROVAL FLOW)
    // ==========================================
    @PostMapping("/export/start")
    public ResponseEntity<?> startExportProcess(
            @RequestParam(defaultValue = "100000000") Double tongTien,
            @RequestParam(defaultValue = "true") Boolean canPheDuyet,
            @RequestParam(defaultValue = "Xuất ấn phẩm mẫu") String lyDoXuat,
            @RequestParam(required = false) String businessKey) {
        try {
            String bKey = businessKey != null ? businessKey : "EXP-" + System.currentTimeMillis();
            Map<String, Object> vars = new HashMap<>();
            vars.put("tongTien", tongTien);
            vars.put("canPheDuyet", canPheDuyet);
            vars.put("lyDoXuat", lyDoXuat);
            vars.put("username", "admin");

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_XuatAnPham", bKey, vars);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Đã khởi chạy Quy trình Xuất ấn phẩm thành công!",
                    "processInstanceId", pi.getId(),
                    "businessKey", bKey));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 2. QUY TRÌNH NHẬP ẤN PHẨM
    // ==========================================
    @PostMapping("/import/start")
    public ResponseEntity<?> startImportProcess(
            @RequestParam(defaultValue = "Nhập ấn phẩm đợt mới") String lyDoNhap,
            @RequestParam(required = false) String businessKey) {
        try {
            String bKey = businessKey != null ? businessKey : "IMP-" + System.currentTimeMillis();
            Map<String, Object> vars = new HashMap<>();
            vars.put("lyDoNhap", lyDoNhap);
            vars.put("username", "admin");

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_NhapAnPham", bKey, vars);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Đã khởi chạy Quy trình Nhập ấn phẩm thành công!",
                    "processInstanceId", pi.getId(),
                    "businessKey", bKey));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 3. QUY TRÌNH CHỐT SỔ CUỐI THÁNG
    // ==========================================
    @PostMapping("/closing/start")
    public ResponseEntity<?> startClosingProcess(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            int y = year != null ? year : java.time.LocalDate.now().getYear();
            int m = month != null ? month : java.time.LocalDate.now().getMonthValue();
            String bKey = "CLOSING-" + y + "-" + String.format("%02d", m);

            Map<String, Object> vars = new HashMap<>();
            vars.put("year", y);
            vars.put("month", m);
            vars.put("username", "admin");

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_ChotSoThang", bKey, vars);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Đã khởi chạy Quy trình Chốt sổ Tháng " + m + "/" + y + " thành công!",
                    "processInstanceId", pi.getId(),
                    "businessKey", bKey));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 4. QUY TRÌNH QUẢN LÝ DANH MỤC ẤN PHẨM
    // ==========================================

    @PostMapping("/catalog/start")
    public ResponseEntity<?> startCatalogProcess(
            @RequestParam(defaultValue = "TAO_MOI") String loaiThaoTac,
            @RequestParam(required = false) String maSo,
            @RequestParam(required = false) String tenHang,
            @RequestParam(required = false, defaultValue = "VPP") String nhomHang,
            @RequestParam(required = false, defaultValue = "Cái") String donViTinh,
            @RequestParam(required = false) Long productId) {
        try {
            Map<String, Object> vars = new HashMap<>();
            if (maSo != null)
                vars.put("maSo", maSo);
            if (tenHang != null)
                vars.put("tenHang", tenHang);
            if (nhomHang != null)
                vars.put("nhomHang", nhomHang);
            if (donViTinh != null)
                vars.put("donViTinh", donViTinh);
            if (productId != null)
                vars.put("productId", productId);
            vars.put("donGiaNhap", java.math.BigDecimal.ZERO);

            String instanceId = camundaWorkflowService.startCatalogWorkflow(loaiThaoTac, vars, "admin");
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Đã khởi chạy Quy trình Quản lý Danh mục Ấn phẩm!",
                    "loaiThaoTac", loaiThaoTac,
                    "processInstanceId", instanceId != null ? instanceId : "N/A"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()));
        }
    }

    // ==========================================
    // 4. QUẢN LÝ USER TASK (DANH SÁCH & PHÊ DUYỆT)
    // ==========================================
    @GetMapping("/tasks")
    public ResponseEntity<?> listActiveTasks(@RequestParam(required = false) String processInstanceId) {
        var query = taskService.createTaskQuery();
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            query.processInstanceId(processInstanceId);
        }

        List<Task> tasks = query.orderByTaskCreateTime().desc().list();
        List<Map<String, Object>> result = tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getId());
            map.put("name", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("createTime", task.getCreateTime());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("executionId", task.getExecutionId());
            map.put("variables", taskService.getVariables(task.getId()));
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<?> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        try {
            Map<String, Object> vars = variables != null ? variables : Map.of();
            camundaWorkflowService.completeUserTask(taskId, vars);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Đã hoàn thành Task " + taskId + " thành công!",
                    "variablesSubmitted", vars));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Demo process có sẵn
    @PostMapping("/start-demo")
    public ResponseEntity<String> startDemoProcess(@RequestParam(defaultValue = "Xin chào Camunda 7!") String message) {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "DemoProcess",
                    Map.of("message", message));
            return ResponseEntity.ok(
                    "Đã khởi tạo DemoProcess thành công! Process Instance ID: " + processInstance.getId()
                            + " (Business Key: " + processInstance.getBusinessKey() + ")");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi khởi chạy DemoProcess: " + e.getMessage());
        }
    }
}
