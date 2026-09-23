package saigonbank.fit.ims.workflow;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import saigonbank.fit.ims.dto.request.ExportDetailRequest;
import saigonbank.fit.ims.dto.request.ExportRequest;
import saigonbank.fit.ims.dto.request.ImportDetailRequest;
import saigonbank.fit.ims.dto.request.ImportRequest;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.repository.InventoryTransactionRepository;
import saigonbank.fit.ims.service.CamundaWorkflowService;
import saigonbank.fit.ims.service.ExportService;
import saigonbank.fit.ims.service.ImportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class CamundaOrchestratorWorkflowTest {

    @Autowired
    private CamundaWorkflowService camundaWorkflowService;

    @Autowired
    private HistoryService historyService;

    @MockitoBean
    private ExportService exportService;

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private InventoryTransactionRepository transactionRepository;

    @Test
    @DisplayName("Kiểm tra luồng Xuất Camunda-driven: Service chỉ được gọi khi Camunda cho phép")
    void testExportProcess_CamundaDriven_Success() {
        // Arrange
        ExportRequest req = new ExportRequest();
        req.setNgayXuat(LocalDate.now());
        req.setNoiNhan("Phòng Kế Toán");
        req.setLyDoXuat("Xuất vật tư quý 3");

        ExportDetailRequest d = new ExportDetailRequest();
        d.setProductId(1L);
        d.setQuantity(new BigDecimal("10"));
        req.setDetails(List.of(d));

        ExportResponse mockResponse = ExportResponse.builder()
                .id(999L)
                .soPhieu("PX-2026-TEST-001")
                .tongTien(new BigDecimal("500000"))
                .ngayXuat(LocalDate.now())
                .build();

        when(exportService.createExport(any(ExportRequest.class), eq("tester"))).thenReturn(mockResponse);

        // Act
        ExportResponse result = camundaWorkflowService.startExportProcess(req, "tester");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSoPhieu()).isEqualTo("PX-2026-TEST-001");

        // Khẳng định: ExportService.createExport được gọi ĐÚNG 1 LẦN thông qua Camunda Delegate
        verify(exportService, times(1)).createExport(any(ExportRequest.class), eq("tester"));

        // Khẳng định: Camunda lịch sử ghi nhận đầy đủ token đi qua các bước BPMN
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .activityId("Task_CapSoVaGhiNhan")
                .list();
        assertThat(activities).isNotEmpty();
    }

    @Test
    @DisplayName("Kiểm tra luồng Nhập Camunda-driven: Service chỉ được gọi khi Camunda cho phép")
    void testImportProcess_CamundaDriven_Success() {
        // Arrange
        ImportRequest req = new ImportRequest();
        req.setNgayNhap(LocalDate.now());
        req.setNoiNhap("Kho vật tư chính");
        req.setNoiMua("Công ty CP Văn phòng phẩm");

        ImportDetailRequest d = new ImportDetailRequest();
        d.setProductId(1L);
        d.setQuantity(new BigDecimal("50"));
        d.setUnitPrice(new BigDecimal("15000"));
        req.setDetails(List.of(d));

        ImportResponse mockResponse = ImportResponse.builder()
                .id(888L)
                .soPhieu("PN-2026-TEST-001")
                .tongTien(new BigDecimal("750000"))
                .ngayNhap(LocalDate.now())
                .build();

        when(importService.createImport(any(ImportRequest.class), eq("tester"))).thenReturn(mockResponse);

        // Act
        ImportResponse result = camundaWorkflowService.startImportProcess(req, "tester");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSoPhieu()).isEqualTo("PN-2026-TEST-001");

        // Khẳng định: ImportService.createImport được gọi ĐÚNG 1 LẦN thông qua Camunda Delegate
        verify(importService, times(1)).createImport(any(ImportRequest.class), eq("tester"));

        // Khẳng định: Camunda lịch sử ghi nhận đầy đủ token đi qua các bước BPMN
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .activityId("Task_CapNhatKhoVaKKTS")
                .list();
        assertThat(activities).isNotEmpty();
    }

    @Test
    @DisplayName("Kiểm tra khi Camunda tắt: Service KHÔNG được phép thực thi, ném lỗi IllegalStateException")
    void testWhenCamundaDisabled_ServiceNeverExecutes() {
        // Tạm thời ngắt RuntimeService để mô phỏng Camunda bị tắt
        RuntimeService originalRuntimeService = (RuntimeService) ReflectionTestUtils.getField(camundaWorkflowService, "runtimeService");
        try {
            ReflectionTestUtils.setField(camundaWorkflowService, "runtimeService", null);

            ExportRequest req = new ExportRequest();
            req.setNgayXuat(LocalDate.now());
            req.setNoiNhan("Phòng Giao Dịch");
            req.setLyDoXuat("Test Camunda Down");

            // Gọi xuất khi Camunda tắt -> PHẢI NÉM LỖI
            assertThatThrownBy(() -> camundaWorkflowService.startExportProcess(req, "tester"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Camunda Workflow Engine");

            // Khẳng định: ExportService TUYỆT ĐỐI KHÔNG được gọi khi Camunda tắt!
            verify(exportService, never()).createExport(any(), any());

            ImportRequest importReq = new ImportRequest();
            importReq.setNgayNhap(LocalDate.now());

            // Gọi nhập khi Camunda tắt -> PHẢI NÉM LỖI
            assertThatThrownBy(() -> camundaWorkflowService.startImportProcess(importReq, "tester"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Camunda Workflow Engine");

            // Khẳng định: ImportService TUYỆT ĐỐI KHÔNG được gọi khi Camunda tắt!
            verify(importService, never()).createImport(any(), any());

        } finally {
            // Khôi phục RuntimeService
            ReflectionTestUtils.setField(camundaWorkflowService, "runtimeService", originalRuntimeService);
        }
    }
}
