package saigonbank.fit.ims.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import saigonbank.fit.ims.dto.request.CreateProductRequest;
import saigonbank.fit.ims.dto.request.UpdateProductRequest;
import saigonbank.fit.ims.dto.response.ProductResponse;
import saigonbank.fit.ims.service.ProductService;

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
        log.info("""
            \n================================================================================
            📦 [CAMUNDA CATALOG STEP - KHỞI TẠO THẺ KHO]
            ▶ Mã hàng               : {} (ID: {})
            ▶ Người thực hiện       : {}
            ▶ Kết quả               : Khởi tạo thẻ kho thành công (tồn ban đầu = 0)
            ================================================================================
            """, maSo, productId, username);
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
        log.info("""
            \n================================================================================
            📊 [CAMUNDA CATALOG STEP - KHỞI TẠO BẢNG KÊ KKTS]
            ▶ Mã hàng               : {} (ID: {})
            ▶ Người thực hiện       : {}
            ▶ Kết quả               : Khởi tạo dòng KKTS thành công (tồn SL=0, tồn tiền=0)
            ================================================================================
            """, maSo, productId, username);
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
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Nhóm hàng không hợp lệ.");
        }
        log.info("[Task_KiemTraDuLieuCapNhat] Dữ liệu cập nhật hợp lệ (tenHang='{}', nhomHang='{}', DVT='{}').",
                tenHang, nhomHang, donViTinh);
    }

    private void handleLuuCapNhat(DelegateExecution execution, String username) {
        Long productId = getLong(execution, "productId");
        if (productId == null) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Thiếu productId khi cập nhật.");
        }
        try {
            UpdateProductRequest req = new UpdateProductRequest();
            req.setTenHang((String) execution.getVariable("tenHang"));
            req.setDonViTinh((String) execution.getVariable("donViTinh"));
            req.setTrangThai((String) execution.getVariable("trangThai"));
            req.setDonGiaNhap(getBigDecimal(execution, "donGiaNhap"));
            productService.updateProduct(productId, req);
            log.info("[Task_LuuCapNhat] Đã lưu cập nhật cho mã hàng ID: {}", productId);
        } catch (Exception e) {
            log.error("[Task_LuuCapNhat] Lỗi khi cập nhật mã hàng: {}", e.getMessage());
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Cập nhật mã hàng thất bại: " + e.getMessage());
        }
    }

    // ─── NHÁNH 3: XÓA HOẶC NGƯNG SỬ DỤNG ────────────────────────────────────

    private void handleKiemTraPhatSinh(DelegateExecution execution) {
        Long productId = getLong(execution, "productId");
        if (productId == null) {
            throw new BpmnError("ERR_INVALID_CATALOG_DATA", "Thiếu productId khi kiểm tra phát sinh.");
        }
        boolean coPhatSinh = productService.hasTransactions(productId);
        execution.setVariable("coPhatSinhGiaoDich", coPhatSinh);
        log.info("[Task_KiemTraPhatSinhGiaoDich] Hàng hóa ID {} có phát sinh giao dịch: {}", productId, coPhatSinh);
    }

    private void handleChuyenTrangThaiNgung(DelegateExecution execution, String username) {
        Long productId = getLong(execution, "productId");
        if (productId != null) {
            productService.deactivateProduct(productId);
            log.info("[Task_ChuyenTrangThaiNgung] Đã chuyển mã hàng ID {} sang NGƯNG SỬ DỤNG.", productId);
        }
    }

    private void handleXoaHoanToanMaHang(DelegateExecution execution, String username) {
        Long productId = getLong(execution, "productId");
        if (productId != null) {
            productService.deleteProduct(productId);
            log.info("[Task_XoaHoanToanMaHang] Đã xóa hoàn toàn mã hàng ID {} (chưa có phát sinh).", productId);
        }
    }

    // ─── AUDIT LOG & HOÀN TẤT TIẾN TRÌNH (CHỈ GHI RA TERMINAL BACKEND) ───────

    private void handleAuditLog(DelegateExecution execution, String username) {
        String loaiThaoTac = (String) execution.getVariable("loaiThaoTac");
        String maSo = firstNonNull(
                (String) execution.getVariable("maSoSaved"),
                (String) execution.getVariable("maSo"),
                "N/A"
        );
        String tenHang = (String) execution.getVariable("tenHang");

        log.info("""
            \n================================================================================
            📋 [CAMUNDA CATALOG RESULT - KẾT QUẢ QUY TRÌNH DANH MỤC ẤN PHẨM]
            ▶ Thao tác              : {}
            ▶ Mã hàng               : {}
            ▶ Tên hàng              : {}
            ▶ Người thực hiện       : {}
            ▶ Process Instance ID   : {}
            ▶ Trạng thái            : Hoàn tất thành công quy trình Camunda
            ================================================================================
            """,
            loaiThaoTac,
            maSo,
            tenHang != null ? tenHang : "N/A",
            username,
            execution.getProcessInstanceId()
        );
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
