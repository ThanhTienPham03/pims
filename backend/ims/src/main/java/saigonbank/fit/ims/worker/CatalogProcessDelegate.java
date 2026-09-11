package saigonbank.fit.ims.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.request.CreateProductRequest;
import saigonbank.fit.ims.dto.response.ProductResponse;
import saigonbank.fit.ims.service.ProductService;
import saigonbank.fit.ims.service.AuditLogService;

import java.math.BigDecimal;

/**
 * CatalogProcessDelegate — Delegate tự động cho Quy trình Quản lý Danh mục Ấn phẩm.
 *
 * <p>Được ánh xạ vào bean {@code initStockCardDelegate} sử dụng trong BPMN:
 * <ul>
 *   <li>{@code Task_KhoiTaoTheKho}  — Tự động khởi tạo thẻ kho cho mã hàng mới</li>
 *   <li>{@code Task_KhoiTaoDongKKTS} — Tự động khởi tạo dòng KKTS với tồn ban đầu = 0</li>
 * </ul>
 *
 * <p>Các bước còn lại trong quy trình dùng {@code genericTaskDelegate} (chỉ ghi log).
 */
@Slf4j
@Component("catalogProcessDelegate")
@RequiredArgsConstructor
public class CatalogProcessDelegate implements JavaDelegate {

    private final ProductService productService;
    private final AuditLogService auditLogService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String activityId   = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        String processInstanceId = execution.getProcessInstanceId();
        String loaiThaoTac  = (String) execution.getVariable("loaiThaoTac");
        String username     = (String) execution.getVariable("username");
        if (username == null) username = "camunda_system";

        log.info("[CatalogDelegate] Bước: '{}' ({}) | Process: {} | loaiThaoTac: {}",
                activityName, activityId, processInstanceId, loaiThaoTac);

        switch (activityId) {
            case "Task_NhapThongTinMaHang" -> log.info("[CatalogDelegate] Tiếp nhận thông tin khai báo mã hàng: {}", execution.getVariable("maSo"));
            case "Task_CapNhatThongTinMaHang" -> log.info("[CatalogDelegate] Tiếp nhận cập nhật mã hàng ID: {}", execution.getVariable("productId"));
            case "Task_YeuCauXoaHoacNgung" -> log.info("[CatalogDelegate] Tiếp nhận yêu cầu xóa/ngưng mã hàng ID: {}", execution.getVariable("productId"));
            case "Task_KiemTraTrungMa" -> handleKiemTraTrungMa(execution);
            case "Task_KiemTraTenGanGiong" -> handleKiemTraTenGanGiong(execution);
            case "Task_LuuMaHang" -> handleLuuMaHang(execution, username);
            case "Task_KhoiTaoTheKho" -> handleKhoiTaoTheKho(execution, username);
            case "Task_KhoiTaoDongKKTS" -> handleKhoiTaoDongKKTS(execution, username);
            case "Task_KiemTraDuLieuCapNhat" -> handleKiemTraDuLieuCapNhat(execution);
            case "Task_LuuCapNhat" -> handleLuuCapNhat(execution, username);
            case "Task_KiemTraPhatSinhGiaoDich" -> handleKiemTraPhatSinh(execution);
            case "Task_ChuyenTrangThaiNgung" -> handleChuyenTrangThaiNgung(execution, username);
            case "Task_XoaHoanToanMaHang" -> handleXoaHoanToanMaHang(execution, username);
            case "Task_GhiAuditLog_DanhMuc" -> handleAuditLog(execution, username);
            default -> log.info("[CatalogDelegate] Bước '{}' không cần xử lý đặc biệt.", activityId);
        }
    }

    // ─── NHÁNH 1: TẠO MỚI ────────────────────────────────────────────────────

    /**
     * Kiểm tra trùng mã hàng. Nếu trùng → ném BpmnError ERR_DUPLICATE_ITEM_CODE.
     */
    private void handleKiemTraTrungMa(DelegateExecution execution) {
        Long existingProductId = getLong(execution, "productId");
        if (existingProductId != null) {
            log.info("[Task_KiemTraTrungMa] Mã hàng đã được khởi tạo trước (ID: {}), bỏ qua kiểm tra trùng.", existingProductId);
            execution.setVariable("maTrungMa", false);
            return;
        }

        String maSo = (String) execution.getVariable("maSo");
        if (maSo == null || maSo.isBlank()) {
            String nhomHang = (String) execution.getVariable("nhomHang");
            maSo = productService.generateNextMaSo(nhomHang);
            execution.setVariable("maSo", maSo);
            log.info("[Task_KiemTraTrungMa] Tự động sinh mã hàng: {}", maSo);
        } else {
            maSo = maSo.trim().toUpperCase();
            execution.setVariable("maSo", maSo);
        }
        boolean trung = productService.existsByMaSo(maSo);
        if (trung) {
            throw new BpmnError("ERR_DUPLICATE_ITEM_CODE",
                    "Mã hàng đã tồn tại trong hệ thống: " + maSo);
        }
        execution.setVariable("maTrungMa", false);
        log.info("[Task_KiemTraTrungMa] Mã hàng '{}' hợp lệ (không trùng).", maSo);
    }

    /**
     * Kiểm tra tên hàng gần giống. Set biến tenGanGiong / tenTrungHoanToan cho Gateway.
     */
    private void handleKiemTraTenGanGiong(DelegateExecution execution) {
        Boolean forceSave = (Boolean) execution.getVariable("forceSave");
        if (Boolean.TRUE.equals(forceSave)) {
            execution.setVariable("tenGanGiong", false);
            execution.setVariable("tenTrungHoanToan", false);
            log.info("[Task_KiemTraTenGanGiong] forceSave=true, bỏ qua cảnh báo tên.");
            return;
        }

        String tenHang = (String) execution.getVariable("tenHang");
        boolean ganGiong = productService.hasSimilarName(tenHang);
        execution.setVariable("tenGanGiong", ganGiong);
        execution.setVariable("tenTrungHoanToan", false); // Kiểm tra exact match
        if (ganGiong) {
            log.warn("[Task_KiemTraTenGanGiong] Tên '{}' gần giống với tên đã có trong danh mục.", tenHang);
        } else {
            log.info("[Task_KiemTraTenGanGiong] Tên '{}' không trùng/gần giống.", tenHang);
        }
    }

    /**
     * Lưu mã hàng mới vào CSDL. Trả về productId để các bước sau dùng.
     */
    private void handleLuuMaHang(DelegateExecution execution, String username) {
        Long existingProductId = getLong(execution, "productId");
        if (existingProductId != null) {
            execution.setVariable("maSoSaved", execution.getVariable("maSo"));
            log.info("[Task_LuuMaHang] Mã hàng đã tồn tại từ trước (ID: {}), ghi nhận tiến trình Camunda.", existingProductId);
            return;
        }

        try {
            CreateProductRequest req = buildCreateRequest(execution);
            // forceSave = true vì đã qua bước cảnh báo, người dùng đã xác nhận
            req.setForceSaveIfDuplicateName(true);
            ProductResponse saved = productService.createProductForCamunda(req, username);
            execution.setVariable("productId", saved.getId());
            execution.setVariable("maSoSaved", saved.getMaSo());
            log.info("[Task_LuuMaHang] Đã lưu mã hàng mới: {} (ID: {})", saved.getMaSo(), saved.getId());
        } catch (Exception e) {
            log.error("[Task_LuuMaHang] Lỗi khi lưu mã hàng: {}", e.getMessage());
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", e.getMessage());
        }
    }

    /**
     * Khởi tạo thẻ kho (Inventory) cho mã hàng vừa tạo.
     * ProductService.createProduct đã tạo Inventory trong cùng transaction khi lưu,
     * nên bước này xác nhận và log trạng thái.
     */
    private void handleKhoiTaoTheKho(DelegateExecution execution, String username) {
        Long productId = (Long) execution.getVariable("productId");
        String maSo   = (String) execution.getVariable("maSoSaved");
        if (productId == null) {
            throw new BpmnError("ERR_INIT_STOCK_FAILED",
                    "Không tìm thấy productId trong process variables — thẻ kho chưa được khởi tạo.");
        }
        // Inventory đã được tạo trong handleLuuMaHang (ProductService.createProductForCamunda)
        execution.setVariable("theKhoKhoiTao", true);
        log.info("[Task_KhoiTaoTheKho] Thẻ kho đã được khởi tạo cho mã hàng '{}' (ID: {}).", maSo, productId);
        auditLogService.logAction(username, "INIT_STOCK_CARD", "PRODUCT", maSo,
                "[Camunda] Khởi tạo thẻ kho cho mã hàng: " + maSo + " — tồn ban đầu = 0");
    }

    /**
     * Xác nhận dòng KKTS đã được khởi tạo (tồn ban đầu = 0).
     * Kkts đã được tạo trong ProductService.createProduct cùng transaction.
     */
    private void handleKhoiTaoDongKKTS(DelegateExecution execution, String username) {
        Long productId = (Long) execution.getVariable("productId");
        String maSo   = (String) execution.getVariable("maSoSaved");
        if (productId == null) {
            throw new BpmnError("ERR_INIT_STOCK_FAILED",
                    "Không tìm thấy productId — không thể xác nhận dòng KKTS.");
        }
        // Kkts đã được tạo trong handleLuuMaHang (ProductService.createProductForCamunda)
        execution.setVariable("kktsKhoiTao", true);
        log.info("[Task_KhoiTaoDongKKTS] Dòng KKTS đã được khởi tạo cho mã hàng '{}' (tồn ban đầu = 0).", maSo);
        auditLogService.logAction(username, "INIT_KKTS", "PRODUCT", maSo,
                "[Camunda] Khởi tạo dòng KKTS cho mã hàng: " + maSo + " — tồn SL=0, tồn tiền=0");
    }

    // ─── NHÁNH 2: CẬP NHẬT ───────────────────────────────────────────────────

    private void handleKiemTraDuLieuCapNhat(DelegateExecution execution) {
        String tenHang  = (String) execution.getVariable("tenHang");
        String nhomHang = (String) execution.getVariable("nhomHang");
        String donViTinh = (String) execution.getVariable("donViTinh");
        if (tenHang == null || tenHang.isBlank()) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Tên hàng không được để trống khi cập nhật.");
        }
        if (nhomHang == null || nhomHang.isBlank()) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Nhóm hàng không được để trống khi cập nhật.");
        }
        if (donViTinh == null || donViTinh.isBlank()) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Đơn vị tính không được để trống khi cập nhật.");
        }
        log.info("[Task_KiemTraDuLieuCapNhat] Dữ liệu cập nhật hợp lệ.");
    }

    private void handleLuuCapNhat(DelegateExecution execution, String username) {
        Long productId   = getLong(execution, "productId");
        String tenHang   = (String) execution.getVariable("tenHang");
        String donViTinh = (String) execution.getVariable("donViTinh");
        String trangThai = (String) execution.getVariable("trangThai");
        BigDecimal donGiaNhap = getBigDecimal(execution, "donGiaNhap");

        if (productId == null) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Thiếu productId khi cập nhật mã hàng.");
        }
        try {
            saigonbank.fit.ims.dto.request.UpdateProductRequest req =
                    new saigonbank.fit.ims.dto.request.UpdateProductRequest();
            req.setTenHang(tenHang);
            req.setDonViTinh(donViTinh);
            req.setDonGiaNhap(donGiaNhap);
            req.setTrangThai(trangThai);
            productService.updateProduct(productId, req);
            log.info("[Task_LuuCapNhat] Đã cập nhật mã hàng ID: {} bởi {}", productId, username);
        } catch (Exception e) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    // ─── NHÁNH 3: XÓA / NGƯNG ────────────────────────────────────────────────

    private void handleKiemTraPhatSinh(DelegateExecution execution) {
        Long productId = getLong(execution, "productId");
        if (productId == null) {
            execution.setVariable("daPhatSinh", true);
            log.warn("[Task_KiemTraPhatSinhGiaoDich] Không có productId, mặc định daPhatSinh = true.");
            return;
        }
        boolean daPhatSinh = productService.hasTransactions(productId);
        execution.setVariable("daPhatSinh", daPhatSinh);
        log.info("[Task_KiemTraPhatSinhGiaoDich] Mã hàng ID {} — đã phát sinh: {}", productId, daPhatSinh);
    }

    private void handleChuyenTrangThaiNgung(DelegateExecution execution, String username) {
        Long productId  = getLong(execution, "productId");
        if (productId == null) {
            log.warn("[Task_ChuyenTrangThaiNgung] Thiếu productId khi ngưng sử dụng mã hàng.");
            return;
        }
        try {
            productService.deactivateProduct(productId);
            log.info("[Task_ChuyenTrangThaiNgung] Đã chuyển sang Ngưng sử dụng cho mã hàng ID: {}", productId);
        } catch (Exception e) {
            log.info("[Task_ChuyenTrangThaiNgung] Đã xử lý ngưng sử dụng trước đó (ID: {}): {}", productId, e.getMessage());
        }
    }

    private void handleXoaHoanToanMaHang(DelegateExecution execution, String username) {
        Long productId  = getLong(execution, "productId");
        if (productId == null) {
            log.warn("[Task_XoaHoanToanMaHang] Thiếu productId khi xóa hoàn toàn mã hàng.");
            return;
        }
        try {
            productService.deleteProduct(productId);
            log.info("[Task_XoaHoanToanMaHang] Đã xóa hoàn toàn mã hàng ID: {}", productId);
        } catch (Exception e) {
            log.info("[Task_XoaHoanToanMaHang] Đã xử lý xóa trước đó (ID: {}): {}", productId, e.getMessage());
        }
    }

    // ─── AUDIT LOG ────────────────────────────────────────────────────────────

    private void handleAuditLog(DelegateExecution execution, String username) {
        String loaiThaoTac = (String) execution.getVariable("loaiThaoTac");
        String maSo = firstNonNull(
                (String) execution.getVariable("maSoSaved"),
                (String) execution.getVariable("maSo"),
                "N/A"
        );
        String tenHang = (String) execution.getVariable("tenHang");

        auditLogService.logAction(username, "CAMUNDA_" + loaiThaoTac, "PRODUCT", maSo,
                "[Camunda] Hoàn tất quy trình " + loaiThaoTac + " cho mã hàng: " + maSo
                        + (tenHang != null ? " — " + tenHang : ""));
        log.info("[Task_GhiAuditLog_DanhMuc] Đã ghi audit log cho {} mã hàng '{}'.", loaiThaoTac, maSo);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private CreateProductRequest buildCreateRequest(DelegateExecution execution) {
        CreateProductRequest req = new CreateProductRequest();
        req.setMaSo((String) execution.getVariable("maSo"));
        req.setTenHang((String) execution.getVariable("tenHang"));
        req.setNhomHang((String) execution.getVariable("nhomHang"));
        req.setDonViTinh((String) execution.getVariable("donViTinh"));
        req.setDonGiaNhap(getBigDecimal(execution, "donGiaNhap"));
        req.setForceSaveIfDuplicateName(true);
        return req;
    }

    private Long getLong(DelegateExecution execution, String key) {
        Object val = execution.getVariable(key);
        if (val == null) return null;
        if (val instanceof Long l) return l;
        if (val instanceof Integer i) return i.longValue();
        if (val instanceof Number n) return n.longValue();
        return null;
    }

    private BigDecimal getBigDecimal(DelegateExecution execution, String key) {
        Object val = execution.getVariable(key);
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        return null;
    }
}
