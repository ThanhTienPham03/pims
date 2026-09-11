package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.repository.InventoryTransactionRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * CamundaWorkflowService — Cầu nối tự động kích hoạt và đồng bộ trạng thái giữa
 * các nghiệp vụ hệ thống (Nhập, Xuất, Khóa sổ) với Camunda 7 Workflow Engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CamundaWorkflowService {

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private TaskService taskService;

    private final InventoryTransactionRepository transactionRepository;

    // Ngưỡng cần Trưởng phòng duyệt phiếu xuất: >= 50.000.000 VNĐ
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("50000000");

    /**
     * Tự động khởi chạy quy trình Xuất ấn phẩm (Process_XuatAnPham)
     */
    @Transactional
    public void startExportWorkflow(ExportResponse response, String username) {
        if (runtimeService == null) {
            log.warn("Camunda RuntimeService không khả dụng, bỏ qua kích hoạt quy trình xuất.");
            return;
        }

        try {
            String soPhieu = response.getSoPhieu();
            BigDecimal tongTien = response.getTongTien() != null ? response.getTongTien() : BigDecimal.ZERO;
            boolean canPheDuyet = tongTien.compareTo(APPROVAL_THRESHOLD) >= 0;

            Map<String, Object> vars = new HashMap<>();
            vars.put("soPhieu", soPhieu);
            vars.put("exportId", response.getId());
            vars.put("tongTien", tongTien.doubleValue());
            vars.put("canPheDuyet", canPheDuyet);
            vars.put("lyDoXuat", response.getLyDoXuat());
            vars.put("username", username);
            vars.put("xuatHetTon", false);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_XuatAnPham", soPhieu, vars);
            log.info("[Camunda Auto] Đã kích hoạt Process_XuatAnPham cho phiếu {}. Instance ID: {}", soPhieu, pi.getId());

            boolean isEnded = pi.isEnded();
            transactionRepository.findById(response.getId()).ifPresent(tx -> {
                tx.setWorkflowKey(pi.getId());
                tx.setWorkflowStatus(canPheDuyet ? "PENDING_APPROVAL" : (isEnded ? "COMPLETED" : "IN_PROGRESS"));
                transactionRepository.save(tx);
            });
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_XuatAnPham cho phiếu {}: {}", response.getSoPhieu(), e.getMessage());
        }
    }

    /**
     * Tự động khởi chạy quy trình Nhập ấn phẩm (Process_NhapAnPham)
     */
    @Transactional
    public void startImportWorkflow(ImportResponse response, String username) {
        if (runtimeService == null) {
            log.warn("Camunda RuntimeService không khả dụng, bỏ qua kích hoạt quy trình nhập.");
            return;
        }

        try {
            String soPhieu = response.getSoPhieu();
            BigDecimal tongTien = response.getTongTien() != null ? response.getTongTien() : BigDecimal.ZERO;

            Map<String, Object> vars = new HashMap<>();
            vars.put("soPhieu", soPhieu);
            vars.put("importId", response.getId());
            vars.put("tongTien", tongTien.doubleValue());
            vars.put("noiNhap", response.getNoiNhap());
            vars.put("username", username);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_NhapAnPham", soPhieu, vars);
            log.info("[Camunda Auto] Đã kích hoạt Process_NhapAnPham cho phiếu {}. Instance ID: {}", soPhieu, pi.getId());

            boolean isEnded = pi.isEnded();
            transactionRepository.findById(response.getId()).ifPresent(tx -> {
                tx.setWorkflowKey(pi.getId());
                tx.setWorkflowStatus(isEnded ? "COMPLETED" : "IN_PROGRESS");
                transactionRepository.save(tx);
            });
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_NhapAnPham cho phiếu {}: {}", response.getSoPhieu(), e.getMessage());
        }
    }

    /**
     * Tự động kích hoạt quy trình Khóa sổ kỳ kế toán (Process_ChotSoThang)
     */
    public void startClosingWorkflow(Integer year, Integer month, String username) {
        if (runtimeService == null) return;
        try {
            String bKey = "CLOSING-" + year + "-" + String.format("%02d", month);
            Map<String, Object> vars = Map.of(
                    "year", year,
                    "month", month,
                    "username", username,
                    "duyetChot", true
            );
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_ChotSoThang", bKey, vars);
            log.info("[Camunda Auto] Đã kích hoạt Process_ChotSoThang cho kỳ {}/{}. Instance ID: {}", month, year, pi.getId());
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_ChotSoThang cho kỳ {}/{}: {}", month, year, e.getMessage());
        }
    }

    /**
     * Khởi chạy quy trình Quản lý Danh mục Ấn phẩm (Process_QuanLyDanhMucAnPham).
     *
     * @param loaiThaoTac  Loại thao tác: {@code TAO_MOI} | {@code CAP_NHAT} | {@code XOA_NGUNG}
     * @param variables    Các biến nghiệp vụ cần truyền vào process (maSo, tenHang, nhomHang, v.v.)
     * @param username     Người thực hiện
     * @return Process Instance ID để frontend theo dõi trạng thái
     */
    public String startCatalogWorkflow(String loaiThaoTac, Map<String, Object> variables, String username) {
        if (runtimeService == null) {
            log.warn("[Camunda Auto] RuntimeService không khả dụng, bỏ qua quy trình danh mục.");
            return null;
        }
        try {
            String maSo = variables.containsKey("maSo") ? (String) variables.get("maSo") : "CATALOG";
            String bKey = "CATALOG-" + loaiThaoTac + "-" + maSo + "-" + System.currentTimeMillis();

            Map<String, Object> vars = new HashMap<>(variables);
            vars.put("loaiThaoTac", loaiThaoTac);
            vars.put("username", username);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                    "Process_QuanLyDanhMucAnPham", bKey, vars);

            log.info("[Camunda Auto] Đã kích hoạt Process_QuanLyDanhMucAnPham | loaiThaoTac={} | maSo={} | Instance ID: {}",
                    loaiThaoTac, maSo, pi.getId());
            return pi.getId();
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_QuanLyDanhMucAnPham ({}): {}",
                    loaiThaoTac, e.getMessage());
            throw new RuntimeException("Không thể khởi chạy quy trình quản lý danh mục: " + e.getMessage(), e);
        }
    }


    /**
     * Hoàn thành User Task và tự động cập nhật trạng thái trong CSDL
     */
    @Transactional
    public void completeUserTask(String taskId, Map<String, Object> variables) {
        if (taskService == null) {
            throw new IllegalStateException("Camunda TaskService không khả dụng.");
        }

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Không tìm thấy Task với ID: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        Map<String, Object> vars = variables != null ? variables : Map.of();
        taskService.complete(taskId, vars);

        if (runtimeService != null) {
            long count = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count();
            if (count == 0) {
                // Toàn bộ quy trình đã kết thúc thành công
                updateStatusByWorkflowKey(processInstanceId, "COMPLETED");
            } else if (Boolean.FALSE.equals(vars.get("duocDuyet")) || Boolean.FALSE.equals(vars.get("duyetChot"))) {
                // Bị từ chối
                updateStatusByWorkflowKey(processInstanceId, "REJECTED");
            }
        }
    }

    private void updateStatusByWorkflowKey(String workflowKey, String status) {
        try {
            transactionRepository.findAll().stream()
                    .filter(tx -> workflowKey.equals(tx.getWorkflowKey()))
                    .findFirst()
                    .ifPresent(tx -> {
                        tx.setWorkflowStatus(status);
                        transactionRepository.save(tx);
                        log.info("[Camunda Auto] Đã cập nhật workflowStatus của phiếu {} thành {}", tx.getTransactionCode(), status);
                    });
        } catch (Exception e) {
            log.warn("[Camunda Auto] Không thể cập nhật workflowStatus: {}", e.getMessage());
        }
    }
}
