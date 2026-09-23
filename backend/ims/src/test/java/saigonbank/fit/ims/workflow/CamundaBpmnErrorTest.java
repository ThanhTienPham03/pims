package saigonbank.fit.ims.workflow;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import saigonbank.fit.ims.service.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class CamundaBpmnErrorTest {

    @Autowired
    private RuntimeService runtimeService;

        @Autowired
        private HistoryService historyService;

        @MockitoBean
        private ProductService productService;

    @Test
    @DisplayName("Test bắt lỗi Camunda: Trùng mã hàng (ERR_DUPLICATE_ITEM_CODE) -> Rẽ vào Boundary Event và dừng ở Error End Event")
    void testDuplicateItemCode_CaughtByBoundaryErrorEvent() {
        // Gỉa lập mã hàng 'STK-001' đã tồn tại trong CSDL
        when(productService.existsByMaSo("STK-001")).thenReturn(true);

        Map<String, Object> vars = new HashMap<>();
        vars.put("loaiThaoTac", "TAO_MOI");
        vars.put("maSo", "STK-001");
        vars.put("tenHang", "Sổ tiết kiệm test");
        vars.put("nhomHang", "STK");
        vars.put("username", "test_admin");

        // Khởi chạy Process_QuanLyDanhMucAnPham
        String businessKey = "TEST-ERR-DUP-" + System.currentTimeMillis();
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                "Process_QuanLyDanhMucAnPham",
                businessKey,
                vars
        );

        // 1. Kiểm tra Process Instance đã kết thúc
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(pi.getId())
                .singleResult();
        assertThat(hpi).isNotNull();
        assertThat(hpi.getEndTime()).as("Quy trình phải kết thúc sau khi bắt lỗi").isNotNull();

        // 2. Lấy toàn bộ danh sách các bước đã chạy (Activity History)
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(pi.getId())
                .list();

        List<String> passedActivityIds = activities.stream()
                .map(HistoricActivityInstance::getActivityId)
                .toList();

        System.out.println("=== CÁC BƯỚC ĐÃ THỰC THI TRONG QUY TRÌNH ===");
        passedActivityIds.forEach(actId -> System.out.println("-> Activity: " + actId));

        // 3. Khẳng định: Đã chạy qua bước kiểm tra mã
        assertThat(passedActivityIds).contains("Task_KiemTraTrungMa");

        // 4. Khẳng định: Camunda đã kích hoạt Boundary Error Event 'Boundary_TrungMa'
        assertThat(passedActivityIds)
                .as("Boundary Error Event 'Boundary_TrungMa' phải được kích hoạt để bắt BpmnError")
                .contains("Boundary_TrungMa");

        // 5. Khẳng định: Quy trình dừng tại Error End Event 'End_LoiTrungMa'
        assertThat(passedActivityIds)
                .as("Quy trình phải dừng lại ở End_LoiTrungMa")
                .contains("End_LoiTrungMa");

        // 6. Khẳng định: Các bước nghiệp vụ tiếp theo (Lưu mã hàng, Tạo thẻ kho) TUYỆT ĐỐI KHÔNG được chạy
        assertThat(passedActivityIds)
                .as("Bước lưu mã hàng không được thực thi khi có lỗi")
                .doesNotContain("Task_LuuMaHang", "Task_KhoiTaoTheKho", "End_ThanhCong");
    }

    @Test
    @DisplayName("Test luồng bình thường: Mã hàng hợp lệ (không trùng) -> Không kích hoạt Boundary Error Event")
    void testValidItemCode_NormalFlow() {
        // Gỉa lập mã hàng 'VPP-MỚI-999' chưa tồn tại trong CSDL
        when(productService.existsByMaSo("VPP-999")).thenReturn(false);
        when(productService.hasSimilarName(anyString())).thenReturn(false);

        Map<String, Object> vars = new HashMap<>();
        vars.put("loaiThaoTac", "TAO_MOI");
        vars.put("maSo", "VPP-999");
        vars.put("tenHang", "Bút bi Thiên Long");
        vars.put("nhomHang", "VPP");
        vars.put("donViTinh", "Cây");
        vars.put("username", "test_admin");
        vars.put("forceSave", true); // để đi thẳng qua cảnh báo tên

        String businessKey = "TEST-NORMAL-" + System.currentTimeMillis();
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                "Process_QuanLyDanhMucAnPham",
                businessKey,
                vars
        );

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(pi.getId())
                .list();

        List<String> passedActivityIds = activities.stream()
                .map(HistoricActivityInstance::getActivityId)
                .toList();

        // Khẳng định: TUYỆT ĐỐI KHÔNG kích hoạt Boundary_TrungMa và End_LoiTrungMa
        assertThat(passedActivityIds).doesNotContain("Boundary_TrungMa", "End_LoiTrungMa");
    }
}
