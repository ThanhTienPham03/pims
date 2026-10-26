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
import saigonbank.fit.ims.dto.request.ExportRequest;
import saigonbank.fit.ims.dto.request.ImportRequest;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.repository.InventoryTransactionRepository;
import saigonbank.fit.ims.worker.ExportProcessDelegate;
import saigonbank.fit.ims.worker.ImportProcessDelegate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CamundaWorkflowService {

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private TaskService taskService;

    @Autowired(required = false)
    private CamundaDmnService camundaDmnService;

    private final InventoryTransactionRepository transactionRepository;
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("50000000");

    @Transactional
    public ExportResponse startExportProcess(ExportRequest request, String username) {
        if (runtimeService == null || taskService == null) {
            log.error("Camunda Workflow Engine không khả dụng. Không thể thực hiện quy trình xuất.");
            throw new IllegalStateException("Hệ thống điều phối quy trình (Camunda Workflow Engine) không khả dụng. "
                    + "Theo kiến trúc nghiệp vụ, phiếu xuất chỉ được tạo khi Camunda cho phép.");
        }

        ExportProcessDelegate.currentExportResponse.remove();
        String bKey = "EXP-" + System.currentTimeMillis();

        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("exportRequest", request);
            vars.put("username", username);
            vars.put("lyDoXuat", request.getLyDoXuat());
            vars.put("xuatHetTon", false);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_XuatAnPham", bKey, vars);
            log.info("[Camunda Driven] Đã khởi chạy Process_XuatAnPham. Instance ID: {}", pi.getId());

            // Tiến hành hoàn thành User Task ban đầu (tiếp nhận thông tin phiếu xuất)
            List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
            for (Task t : activeTasks) {
                if ("Task_NhapThongTinPhieuXuat".equals(t.getTaskDefinitionKey())) {
                    taskService.complete(t.getId());
                }
            }

            ExportResponse response = ExportProcessDelegate.currentExportResponse.get();
            if (response == null) {
                log.warn(
                        "[Camunda Driven] Quy trình xuất hoàn tất nhưng chưa có ExportResponse (có thể đang chờ phê duyệt hoặc kết thúc sớm)");
            }
            return response;
        } catch (Exception e) {
            log.error("[Camunda Driven] Lỗi trong quy trình xuất: {}", e.getMessage());
            unwrapAndRethrowBusinessException(e);
            throw new RuntimeException("Lỗi trong quy trình xuất ấn phẩm: " + e.getMessage(), e);
        } finally {
            ExportProcessDelegate.currentExportResponse.remove();
        }
    }

    @Transactional
    public ImportResponse startImportProcess(ImportRequest request, String username) {
        if (runtimeService == null || taskService == null) {
            log.error("Camunda Workflow Engine không khả dụng. Không thể thực hiện quy trình nhập.");
            throw new IllegalStateException("Hệ thống điều phối quy trình (Camunda Workflow Engine) không khả dụng. "
                    + "Theo kiến trúc nghiệp vụ, phiếu nhập chỉ được tạo khi Camunda cho phép.");
        }

        ImportProcessDelegate.currentImportResponse.remove();
        String bKey = "IMP-" + System.currentTimeMillis();

        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("importRequest", request);
            vars.put("username", username);
            vars.put("noiNhap", request.getNoiNhap());

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_NhapAnPham", bKey, vars);
            log.info("[Camunda Driven] Đã khởi chạy Process_NhapAnPham. Instance ID: {}", pi.getId());

            // Tự động hoàn thành các bước nhập dữ liệu ban đầu trên form để tiến đến bước
            // thẩm định và ghi nhận
            int maxSteps = 10;
            while (maxSteps-- > 0) {
                List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
                if (activeTasks.isEmpty()) {
                    break;
                }
                boolean progressed = false;
                for (Task t : activeTasks) {
                    String key = t.getTaskDefinitionKey();
                    if ("Task_NhapThongTinChung".equals(key)
                            || "Task_ChonHangChiTiet".equals(key)
                            || "Task_NhapSoLuongDonGia".equals(key)
                            || "Task_GhiNhanPhieu".equals(key)) {
                        taskService.complete(t.getId());
                        progressed = true;
                    }
                }
                if (!progressed) {
                    break;
                }
            }

            ImportResponse response = ImportProcessDelegate.currentImportResponse.get();
            if (response == null) {
                log.warn("[Camunda Driven] Quy trình nhập hoàn tất nhưng chưa có ImportResponse");
            }
            return response;
        } catch (Exception e) {
            log.error("[Camunda Driven] Lỗi trong quy trình nhập: {}", e.getMessage());
            unwrapAndRethrowBusinessException(e);
            throw new RuntimeException("Lỗi trong quy trình nhập ấn phẩm: " + e.getMessage(), e);
        } finally {
            ImportProcessDelegate.currentImportResponse.remove();
        }
    }

    private void unwrapAndRethrowBusinessException(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof saigonbank.fit.ims.exception.InsufficientStockException ise) {
                throw ise;
            }
            if (current instanceof saigonbank.fit.ims.exception.MonthLockedException mle) {
                throw mle;
            }
            if (current instanceof saigonbank.fit.ims.exception.AcqtMixingException ame) {
                throw ame;
            }
            if (current instanceof saigonbank.fit.ims.exception.InvalidExportException iee) {
                throw iee;
            }
            if (current instanceof saigonbank.fit.ims.exception.InvalidImportException iie) {
                throw iie;
            }
            if (current instanceof saigonbank.fit.ims.exception.ResourceNotFoundException rne) {
                throw rne;
            }
            if (current instanceof saigonbank.fit.ims.exception.ProductNotFoundException pne) {
                throw pne;
            }
            if (current.getCause() == current)
                break;
            current = current.getCause();
        }
    }

    /**
     * @deprecated Sử dụng {@link #startExportProcess(ExportRequest, String)} để
     *             Camunda điều phối thực sự.
     */
    @Deprecated
    @Transactional
    public void startExportWorkflow(ExportResponse response, String username) {
        if (runtimeService == null) {
            log.warn("Camunda RuntimeService không khả dụng, bỏ qua kích hoạt quy trình xuất.");
            return;
        }

        try {
            String soPhieu = response.getSoPhieu();
            BigDecimal tongTien = response.getTongTien() != null ? response.getTongTien() : BigDecimal.ZERO;
            String nhomHang = response.getNhomDaiDien() != null ? response.getNhomDaiDien() : "VPP";

            boolean canPheDuyet;
            String capPheDuyet = "TU_DONG";
            int hanDuyetGio = 0;
            String moTaLyDo = "Tự động duyệt";

            if (camundaDmnService != null) {
                var decision = camundaDmnService.evaluateExportApproval(nhomHang, tongTien, response.getLyDoXuat());
                canPheDuyet = decision.getCanPheDuyet();
                capPheDuyet = decision.getCapPheDuyet();
                hanDuyetGio = decision.getHanDuyetGio();
                moTaLyDo = decision.getMoTaLyDo();
            } else {
                canPheDuyet = "ACQT".equalsIgnoreCase(nhomHang) || tongTien.compareTo(APPROVAL_THRESHOLD) >= 0;
                capPheDuyet = canPheDuyet ? "TRUONG_PHONG" : "TU_DONG";
            }

            Map<String, Object> vars = new HashMap<>();
            vars.put("soPhieu", soPhieu);
            vars.put("exportId", response.getId());
            vars.put("tongTien", tongTien.doubleValue());
            vars.put("nhomHang", nhomHang);
            vars.put("canPheDuyet", canPheDuyet);
            vars.put("capPheDuyet", capPheDuyet);
            vars.put("hanDuyetGio", hanDuyetGio);
            vars.put("moTaLyDo", moTaLyDo);
            vars.put("lyDoXuat", response.getLyDoXuat());
            vars.put("username", username);
            vars.put("xuatHetTon", false);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_XuatAnPham", soPhieu, vars);
            log.info(
                    "[Camunda Auto] Đã kích hoạt Process_XuatAnPham cho phiếu {}. Instance ID: {} | Cần duyệt: {} ({})",
                    soPhieu, pi.getId(), canPheDuyet, capPheDuyet);

            boolean isEnded = pi.isEnded();
            transactionRepository.findById(response.getId()).ifPresent(tx -> {
                tx.setWorkflowKey(pi.getId());
                tx.setWorkflowStatus(canPheDuyet ? "PENDING_APPROVAL" : (isEnded ? "COMPLETED" : "IN_PROGRESS"));
                transactionRepository.save(tx);
            });
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_XuatAnPham cho phiếu {}: {}", response.getSoPhieu(),
                    e.getMessage());
        }
    }

    @Deprecated
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
            log.info("[Camunda Auto] Đã kích hoạt Process_NhapAnPham cho phiếu {}. Instance ID: {}", soPhieu,
                    pi.getId());

            boolean isEnded = pi.isEnded();
            transactionRepository.findById(response.getId()).ifPresent(tx -> {
                tx.setWorkflowKey(pi.getId());
                tx.setWorkflowStatus(isEnded ? "COMPLETED" : "IN_PROGRESS");
                transactionRepository.save(tx);
            });
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_NhapAnPham cho phiếu {}: {}", response.getSoPhieu(),
                    e.getMessage());
        }
    }

    public void startClosingWorkflow(Integer year, Integer month, String username) {
        if (runtimeService == null)
            return;
        try {
            String bKey = "CLOSING-" + year + "-" + String.format("%02d", month);
            Map<String, Object> vars = Map.of(
                    "year", year,
                    "month", month,
                    "username", username,
                    "duyetChot", true);
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("Process_ChotSoThang", bKey, vars);
            log.info("[Camunda Auto] Đã kích hoạt Process_ChotSoThang cho kỳ {}/{}. Instance ID: {}", month, year,
                    pi.getId());
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_ChotSoThang cho kỳ {}/{}: {}", month, year,
                    e.getMessage());
        }
    }

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
            vars.putIfAbsent("daPhatSinh", false);
            vars.putIfAbsent("tenGanGiong", false);
            vars.putIfAbsent("tenTrungHoanToan", false);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                    "Process_QuanLyDanhMucAnPham", bKey, vars);

            log.info(
                    "[Camunda Auto] Đã kích hoạt Process_QuanLyDanhMucAnPham | loaiThaoTac={} | maSo={} | Instance ID: {}",
                    loaiThaoTac, maSo, pi.getId());
            return pi.getId();
        } catch (Exception e) {
            log.error("[Camunda Auto] Lỗi khi kích hoạt Process_QuanLyDanhMucAnPham ({}): {}",
                    loaiThaoTac, e.getMessage());
            throw new RuntimeException("Không thể khởi chạy quy trình quản lý danh mục: " + e.getMessage(), e);
        }
    }

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
                        log.info("[Camunda Auto] Đã cập nhật workflowStatus của phiếu {} thành {}",
                                tx.getTransactionCode(), status);
                    });
        } catch (Exception e) {
            log.warn("[Camunda Auto] Không thể cập nhật workflowStatus: {}", e.getMessage());
        }
    }
}
