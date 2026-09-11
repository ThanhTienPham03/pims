package saigonbank.fit.ims.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.MonthlyClosingResponse;
import saigonbank.fit.ims.service.MonthlyClosingService;

import java.util.List;

@RestController
@RequestMapping("/api/monthly-closings")
@RequiredArgsConstructor
public class MonthlyClosingController {

    private final MonthlyClosingService monthlyClosingService;
    private final saigonbank.fit.ims.service.CamundaWorkflowService camundaWorkflowService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MonthlyClosingResponse>>> getAllClosings() {
        List<MonthlyClosingResponse> response = monthlyClosingService.getAllClosings();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/lock")
    @PreAuthorize("hasRole('CONTROLLER')")
    public ResponseEntity<ApiResponse<MonthlyClosingResponse>> lockMonth(
            @RequestParam Integer year,
            @RequestParam Integer month,
            Authentication authentication) {
        String username = authentication.getName();
        MonthlyClosingResponse response = monthlyClosingService.lockMonth(year, month, username);
        // Tự động kích hoạt quy trình Camunda
        camundaWorkflowService.startClosingWorkflow(year, month, username);
        return ResponseEntity.ok(ApiResponse.success("Đã khóa sổ tháng " + month + "/" + year, response));
    }

    @PostMapping("/unlock")
    @PreAuthorize("hasRole('CONTROLLER')")
    public ResponseEntity<ApiResponse<MonthlyClosingResponse>> unlockMonth(
            @RequestParam Integer year,
            @RequestParam Integer month,
            Authentication authentication) {
        String username = authentication.getName();
        MonthlyClosingResponse response = monthlyClosingService.unlockMonth(year, month, username);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khóa sổ tháng " + month + "/" + year, response));
    }
}
