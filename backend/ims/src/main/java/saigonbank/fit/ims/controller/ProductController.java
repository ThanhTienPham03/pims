package saigonbank.fit.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import saigonbank.fit.ims.dto.request.CreateProductRequest;
import saigonbank.fit.ims.dto.request.UpdateProductRequest;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.dto.response.ProductResponse;
import saigonbank.fit.ims.entity.ProductGroup;
import saigonbank.fit.ims.entity.Unit;
import saigonbank.fit.ims.repository.ProductGroupRepository;
import saigonbank.fit.ims.repository.UnitRepository;
import saigonbank.fit.ims.service.CamundaWorkflowService;
import saigonbank.fit.ims.service.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductGroupRepository productGroupRepository;
    private final UnitRepository unitRepository;
    private final CamundaWorkflowService camundaWorkflowService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String maSo,
            @RequestParam(required = false) String tenHang,
            @RequestParam(required = false) String nhomHang,
            @RequestParam(required = false) String trangThai,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ProductResponse> response = productService.getProducts(maSo, tenHang, nhomHang, trangThai,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getActiveProducts() {
        List<ProductResponse> response = productService.getActiveProducts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/next-code")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> getNextCode(
            @RequestParam(defaultValue = "VPP") String nhomHang) {
        String nextCode = productService.generateNextMaSo(nhomHang);
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("maSo", nextCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/migrate-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> migrateLegacyCodes() {
        int count = productService.migrateAllLegacyProductCodes();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of(
                "migratedCount", count,
                "message", "Đã chuẩn hóa " + count + " mã sản phẩm sang định dạng mới.")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        ProductResponse response = productService.createProduct(request, username);
        if (response.isNameWarning()) {
            return ResponseEntity.ok(ApiResponse.success(response.getWarningMessage(), response));
        }
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("productId", response.getId());
            vars.put("maSo", response.getMaSo());
            vars.put("tenHang", response.getTenHang());
            vars.put("nhomHang", response.getNhomHang());
            vars.put("donViTinh", response.getDonViTinh());
            vars.put("donGiaNhap",
                    response.getDonGiaNhap() != null ? response.getDonGiaNhap() : java.math.BigDecimal.ZERO);
            vars.put("forceSave", Boolean.TRUE.equals(request.getForceSaveIfDuplicateName()));
            camundaWorkflowService.startCatalogWorkflow("TAO_MOI", vars, username);
        } catch (Exception e) {
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm hàng hóa thành công", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        ProductResponse response = productService.updateProduct(id, request);

        // Tự động kích hoạt Camunda workflow
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("productId", id);
            vars.put("tenHang", request.getTenHang());
            vars.put("donViTinh", request.getDonViTinh());
            vars.put("trangThai", request.getTrangThai());
            vars.put("nhomHang", response.getNhomHang());
            vars.put("donGiaNhap",
                    request.getDonGiaNhap() != null ? request.getDonGiaNhap() : java.math.BigDecimal.ZERO);
            camundaWorkflowService.startCatalogWorkflow("CAP_NHAT", vars, username);
        } catch (Exception e) {
        }

        return ResponseEntity.ok(ApiResponse.success("Cập nhật hàng hóa thành công", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        productService.deleteProduct(id);

        // Tự động kích hoạt Camunda workflow
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("productId", id);
            camundaWorkflowService.startCatalogWorkflow("XOA_NGUNG", vars, username);
        } catch (Exception e) {
        }

        return ResponseEntity.ok(ApiResponse.success("Đã xóa hàng hóa thành công", null));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        productService.deactivateProduct(id, username);

        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("productId", id);
            camundaWorkflowService.startCatalogWorkflow("NGUNG_SU_DUNG", vars, username);
        } catch (Exception e) {
        }

        return ResponseEntity.ok(ApiResponse.success("Đã ngừng sử dụng hàng hóa", null));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateProduct(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        productService.activateProduct(id, username);

        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt lại hàng hóa", null));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<ProductGroup>>> getProductGroups() {
        List<ProductGroup> groups = productGroupRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/units")
    public ResponseEntity<ApiResponse<List<Unit>>> getUnits() {
        List<Unit> units = unitRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(units));
    }

    @PostMapping("/workflow")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createProductViaWorkflow(
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            if (request.getMaSo() == null || request.getMaSo().isBlank()) {
                request.setMaSo(productService.generateNextMaSo(request.getNhomHang()));
            } else {
                request.setMaSo(request.getMaSo().trim().toUpperCase());
            }

            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("maSo", request.getMaSo());
            vars.put("tenHang", request.getTenHang());
            vars.put("nhomHang", request.getNhomHang());
            vars.put("donViTinh", request.getDonViTinh());
            vars.put("donGiaNhap", request.getDonGiaNhap() != null
                    ? request.getDonGiaNhap()
                    : java.math.BigDecimal.ZERO);
            // Nếu forceSave = true (từ frontend sau cảnh báo tên), bỏ qua User Task
            vars.put("tenGanGiong", false);
            vars.put("tenTrungHoanToan", false);

            String processInstanceId = camundaWorkflowService
                    .startCatalogWorkflow("TAO_MOI", vars, username);

            return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("Quy trình tạo mã hàng đã được khởi chạy.",
                            java.util.Map.of(
                                    "processInstanceId", processInstanceId != null ? processInstanceId : "N/A",
                                    "maSo", request.getMaSo())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Khởi chạy quy trình thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/workflow")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateProductViaWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("productId", id);
            vars.put("tenHang", request.getTenHang());
            vars.put("donViTinh", request.getDonViTinh());
            vars.put("trangThai", request.getTrangThai());
            vars.put("donGiaNhap", request.getDonGiaNhap() != null
                    ? request.getDonGiaNhap()
                    : java.math.BigDecimal.ZERO);
            // Lấy nhomHang từ product hiện có (cần thiết cho bước kiểm tra)
            try {
                ProductResponse existing = productService.getProductById(id);
                vars.put("nhomHang", existing.getNhomHang());
            } catch (Exception ignored) {
                vars.put("nhomHang", "VPP"); // fallback
            }

            String processInstanceId = camundaWorkflowService
                    .startCatalogWorkflow("CAP_NHAT", vars, username);

            return ResponseEntity.accepted()
                    .body(ApiResponse.success("Quy trình cập nhật mã hàng đã được khởi chạy.",
                            java.util.Map.of(
                                    "processInstanceId", processInstanceId != null ? processInstanceId : "N/A",
                                    "productId", id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Khởi chạy quy trình thất bại: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/workflow")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteProductViaWorkflow(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            java.util.Map<String, Object> vars = java.util.Map.of("productId", id);

            String processInstanceId = camundaWorkflowService
                    .startCatalogWorkflow("XOA_NGUNG", new java.util.HashMap<>(vars), username);

            return ResponseEntity.accepted()
                    .body(ApiResponse.success("Quy trình xóa/ngưng mã hàng đã được khởi chạy.",
                            java.util.Map.of(
                                    "processInstanceId", processInstanceId != null ? processInstanceId : "N/A",
                                    "productId", id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Khởi chạy quy trình thất bại: " + e.getMessage()));
        }
    }
}
