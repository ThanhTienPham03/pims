package saigonbank.fit.ims.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.KktsResponse;
import saigonbank.fit.ims.service.KktsService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/kkts")
@RequiredArgsConstructor
public class KktsController {

    private final KktsService kktsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<KktsResponse>>> getKktsByPeriod(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        List<KktsResponse> response = kktsService.getKktsByPeriod(y, m);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
