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
import saigonbank.fit.ims.dto.request.ImportRequest;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.service.ImportService;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {
    private final ImportService importService;
    private final saigonbank.fit.ims.service.CamundaWorkflowService camundaWorkflowService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ImportResponse>>> getImports(
            @RequestParam(required = false) String soPhieu,
            @RequestParam(required = false) String noiMua,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ImportResponse> response = importService.getImports(soPhieu, noiMua, fromDate, toDate, trangThai,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImportResponse>> getImportById(@PathVariable Long id) {
        ImportResponse response = importService.getImportById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<ImportResponse>> createImport(
            @Valid @RequestBody ImportRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Quy trình nhập được khởi chạy và điều phối bởi Camunda; ImportService chỉ được gọi qua Delegate của BPMN
        ImportResponse response = camundaWorkflowService.startImportProcess(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phiếu nhập thành công", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportResponse>> cancelImport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ImportResponse response = importService.cancelImport(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Hủy phiếu nhập thành công", response));
    }
}
