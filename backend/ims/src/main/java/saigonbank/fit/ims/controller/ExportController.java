package saigonbank.fit.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import saigonbank.fit.ims.config.security.UserDetailsImpl;
import saigonbank.fit.ims.dto.request.ExportRequest;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.service.ExportService;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final saigonbank.fit.ims.service.CamundaWorkflowService camundaWorkflowService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExportResponse>>> getExports(
            @RequestParam(required = false) String soPhieu,
            @RequestParam(required = false) String noiNhan,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ExportResponse> response = exportService.getExports(soPhieu, noiNhan, fromDate, toDate, trangThai, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExportResponse>> getExportById(@PathVariable Long id) {
        ExportResponse response = exportService.getExportById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<ExportResponse>> createExport(
            @Valid @RequestBody ExportRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Quy trình xuất được khởi chạy và điều phối bởi Camunda; ExportService chỉ được gọi qua Delegate của BPMN
        ExportResponse response = camundaWorkflowService.startExportProcess(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo phiếu xuất thành công", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<ExportResponse>> cancelExport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ExportResponse response = exportService.cancelExport(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Hủy phiếu xuất thành công", response));
    }
}
