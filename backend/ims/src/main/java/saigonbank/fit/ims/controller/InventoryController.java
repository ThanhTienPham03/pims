package saigonbank.fit.ims.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.InventoryResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.dto.response.StockCardResponse;
import saigonbank.fit.ims.service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getInventoryList(
            @RequestParam(required = false) String maSo,
            @RequestParam(required = false) String tenHang,
            @RequestParam(required = false) String nhomHang,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<InventoryResponse> response = inventoryService.getInventoryList(maSo, tenHang, nhomHang, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getInventorySummary() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventorySummary()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProduct(@PathVariable Long productId) {
        InventoryResponse response = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{productId}/stock-card")
    public ResponseEntity<ApiResponse<StockCardResponse>> getStockCard(@PathVariable Long productId) {
        StockCardResponse response = inventoryService.getStockCard(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
