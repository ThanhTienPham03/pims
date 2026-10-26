package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.request.CreateProductRequest;
import saigonbank.fit.ims.dto.request.UpdateProductRequest;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.dto.response.ProductResponse;
import saigonbank.fit.ims.entity.*;
import saigonbank.fit.ims.exception.ProductAlreadyExistsException;
import saigonbank.fit.ims.exception.ProductNotFoundException;
import saigonbank.fit.ims.exception.ProductDeletionNotAllowedException;
import saigonbank.fit.ims.exception.ResourceNotFoundException;
import saigonbank.fit.ims.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductService — quản lý sản phẩm.
 * Khi tạo sản phẩm mới: tạo đồng thời Inventory + StockCardEntry(OPEN) + Kkts cho tháng hiện tại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductGroupRepository productGroupRepository;
    private final UnitRepository unitRepository;
    private final InventoryRepository inventoryRepository;
    private final KktsRepository kktsRepository;
    private final InventoryTransactionDetailRepository inventoryTransactionDetailRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public String generateNextMaSo(String nhomHang) {
        String prefix = getProductCodePrefix(nhomHang);
        List<String> existingCodes = productRepository.findMaSoByPrefix(prefix);
        int maxSeq = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)$");
        for (String code : existingCodes) {
            if (code == null) continue;
            String clean = code.trim().toUpperCase();
            if (clean.startsWith(prefix.toUpperCase())) {
                String remainder = clean.substring(prefix.length());
                java.util.regex.Matcher matcher = pattern.matcher(remainder);
                if (matcher.find()) {
                    try {
                        int num = Integer.parseInt(matcher.group(1));
                        if (num > maxSeq) {
                            maxSeq = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return String.format("%s%03d", prefix, maxSeq + 1);
    }

    @Transactional
    public int migrateAllLegacyProductCodes() {
        List<Product> products = productRepository.findAll();
        java.util.regex.Pattern numPattern = java.util.regex.Pattern.compile("(\\d+)$");
        int count = 0;

        for (Product product : products) {
            String currentMaSo = product.getMaSo();
            if (currentMaSo == null || currentMaSo.startsWith("32F.")) {
                continue;
            }

            String groupCode = (product.getProductGroup() != null && product.getProductGroup().getCode() != null)
                    ? product.getProductGroup().getCode()
                    : null;
            if (groupCode == null) {
                String clean = currentMaSo.toUpperCase();
                if (clean.startsWith("ACTT")) groupCode = "ACTT";
                else if (clean.startsWith("ACQT")) groupCode = "ACQT";
                else if (clean.startsWith("VPP")) groupCode = "VPP";
                else if (clean.startsWith("VLK")) groupCode = "VLK";
                else groupCode = "VPP";
            }

            String prefix = getProductCodePrefix(groupCode);

            java.util.regex.Matcher matcher = numPattern.matcher(currentMaSo.trim());
            int targetSeq = -1;
            if (matcher.find()) {
                try {
                    targetSeq = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {}
            }

            String newMaSo;
            if (targetSeq > 0 && !productRepository.existsByMaSo(String.format("%s%03d", prefix, targetSeq))) {
                newMaSo = String.format("%s%03d", prefix, targetSeq);
            } else {
                newMaSo = generateNextMaSo(groupCode);
            }

            product.setMaSo(newMaSo);
            productRepository.save(product);
            auditLogService.logAction("SYSTEM", "MIGRATE", "PRODUCT", newMaSo,
                    "Chuyển đổi mã hàng từ " + currentMaSo + " sang " + newMaSo + " (" + product.getTenHang() + ")");
            log.info("Migrated legacy product code: [{}] -> [{}] ({})", currentMaSo, newMaSo, product.getTenHang());
            count++;
        }
        return count;
    }

    public static String getProductCodePrefix(String nhomHang) {
        if (nhomHang == null || nhomHang.isBlank()) {
            return "32F.7100001.";
        }
        String clean = nhomHang.trim().toUpperCase();
        if (clean.startsWith("32F.")) {
            return clean.endsWith(".") ? clean : clean + ".";
        }
        return switch (clean) {
            case "ACTT" -> "32F.9020100.";
            case "VPP"  -> "32F.7100001.";
            case "ACQT" -> "32F.5010000.";
            case "VLK"  -> "32F.9080117.";
            default     -> clean + ".";
        };
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, String username) {
        if (request.getMaSo() == null || request.getMaSo().isBlank()) {
            request.setMaSo(generateNextMaSo(request.getNhomHang()));
        } else {
            request.setMaSo(request.getMaSo().trim().toUpperCase());
        }

        if (productRepository.existsByMaSo(request.getMaSo())) {
            throw new ProductAlreadyExistsException("Mã số hàng hóa đã tồn tại: " + request.getMaSo());
        }

        if (!Boolean.TRUE.equals(request.getForceSaveIfDuplicateName())) {
            List<Product> similar = productRepository.findSimilarName(request.getTenHang(), -1L);
            if (!similar.isEmpty()) {
                String existingNames = similar.stream()
                        .map(p -> p.getMaSo() + " (" + p.getTenHang() + ")")
                        .limit(3)
                        .collect(Collectors.joining(", "));
                String warningMessage = "Cảnh báo: Tên hàng đã tồn tại hoặc gần giống với: " + existingNames
                        + (similar.size() > 3 ? " và " + (similar.size() - 3) + " sản phẩm khác." : ".");

                log.warn("Cảnh báo tên hàng gần giống/trùng: '{}'. Đã có: {}. Chưa lưu vào DB.", request.getTenHang(), existingNames);
                return ProductResponse.builder()
                        .nameWarning(true)
                        .warningMessage(warningMessage)
                        .build();
            }
        }


        ProductGroup productGroup = productGroupRepository.findByCode(request.getNhomHang())
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm hàng không tồn tại: " + request.getNhomHang()));

        Unit unit = unitRepository.findByCode(request.getDonViTinh())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị tính không tồn tại: " + request.getDonViTinh()));

        Product product = Product.builder()
                .maSo(request.getMaSo())
                .tenHang(request.getTenHang())
                .productGroup(productGroup)
                .unit(unit)
                .donGiaNhap(request.getDonGiaNhap())
                .build();
        product = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(product)
                .build();
        inventoryRepository.save(inventory);

        LocalDate now = LocalDate.now();
        Kkts kkts = Kkts.builder()
                .product(product)
                .year(now.getYear())
                .month(now.getMonthValue())
                .build();
        kktsRepository.save(kkts);

        log.info("Created product: {} by {}", product.getMaSo(), username);
        auditLogService.logAction(username, "CREATE", "PRODUCT", product.getMaSo(),
                "Tạo mã hàng mới: " + product.getMaSo() + " — " + product.getTenHang());
        return mapToResponse(product, false, null);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        Unit unit = unitRepository.findByCode(request.getDonViTinh())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị tính không tồn tại: " + request.getDonViTinh()));

        product.setTenHang(request.getTenHang());
        product.setUnit(unit);
        if (request.getDonGiaNhap() != null) {
            product.setDonGiaNhap(request.getDonGiaNhap());
        }
        if (request.getTrangThai() != null) {
            product.setTrangThai(request.getTrangThai());
        }

        product = productRepository.save(product);
        auditLogService.logAction(null, "UPDATE", "PRODUCT", product.getMaSo(),
                "Cập nhật thông tin mã hàng: " + product.getMaSo() + " — " + product.getTenHang());
        return mapToResponse(product, false, null);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        boolean hasTransactions = inventoryTransactionDetailRepository.existsByProductId(id);
        if (hasTransactions) {
            throw new ProductDeletionNotAllowedException("Mã hàng đã phát sinh giao dịch nhập/xuất, không được xóa. Vui lòng chuyển trạng thái sang Ngưng sử dụng.");
        }

        kktsRepository.deleteByProductId(id);
        inventoryRepository.deleteByProductId(id);
        productRepository.delete(product);
        log.info("Deleted product: {}", product.getMaSo());
        auditLogService.logAction(null, "DELETE", "PRODUCT", product.getMaSo(),
                "Xóa mã hàng: " + product.getMaSo() + " — " + product.getTenHang());
    }

    @Transactional
    public void deactivateProduct(Long id) {
        deactivateProduct(id, null);
    }

    @Transactional
    public void deactivateProduct(Long id, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        product.setTrangThai(Product.ProductStatus.NGUNG_SU_DUNG);
        productRepository.save(product);
        log.info("Deactivated product: {}", product.getMaSo());
        auditLogService.logAction(username, "DEACTIVATE", "PRODUCT", product.getMaSo(),
                "Ngưng sử dụng mã hàng: " + product.getMaSo() + " — " + product.getTenHang());
    }

    @Transactional
    public void activateProduct(Long id) {
        activateProduct(id, null);
    }

    @Transactional
    public void activateProduct(Long id, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        product.setTrangThai(Product.ProductStatus.HOAT_DONG);
        productRepository.save(product);
        log.info("Activated product: {}", product.getMaSo());
        auditLogService.logAction(username, "ACTIVATE", "PRODUCT", product.getMaSo(),
                "Kích hoạt lại mã hàng: " + product.getMaSo() + " — " + product.getTenHang());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return mapToResponse(product, false, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(String maSo, String tenHang,
                                                      String nhomHang, String trangThai,
                                                      Pageable pageable) {
        Page<Product> page = productRepository.findWithFilters(maSo, tenHang, nhomHang, trangThai, pageable);
        List<ProductResponse> content = page.getContent().stream()
                .map(p -> mapToResponse(p, false, null))
                .toList();
        return PageResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findAllActive().stream()
                .map(p -> mapToResponse(p, false, null))
                .toList();
    }

    // ─── Helper methods dùng bởi CatalogProcessDelegate (Camunda) ──────────────

    /**
     * Kiểm tra mã hàng đã tồn tại (kể cả Ngưng sử dụng).
     * Dùng bởi BPMN Task_KiemTraTrungMa.
     */
    @Transactional(readOnly = true)
    public boolean existsByMaSo(String maSo) {
        return productRepository.existsByMaSo(maSo);
    }

    /**
     * Kiểm tra tên hàng có gần giống với bất kỳ tên nào đã có không.
     * Dùng bởi BPMN Task_KiemTraTenGanGiong.
     */
    @Transactional(readOnly = true)
    public boolean hasSimilarName(String tenHang) {
        if (tenHang == null || tenHang.isBlank()) return false;
        return !productRepository.findSimilarName(tenHang, -1L).isEmpty();
    }

    /**
     * Kiểm tra mã hàng đã có giao dịch nhập/xuất chưa.
     * Dùng bởi BPMN Task_KiemTraPhatSinhGiaoDich.
     */
    @Transactional(readOnly = true)
    public boolean hasTransactions(Long productId) {
        return inventoryTransactionDetailRepository.existsByProductId(productId);
    }

    /**
     * Tạo mã hàng mới từ Camunda workflow — luôn lưu (cảnh báo tên đã được
     * xử lý ở tầng BPMN qua User Task Task_CanhBaoTenTrung).
     */
    @Transactional
    public ProductResponse createProductForCamunda(CreateProductRequest request, String username) {
        // Đảm bảo bỏ qua cảnh báo tên (Camunda đã xử lý)
        request.setForceSaveIfDuplicateName(true);
        return createProduct(request, username);
    }

    private ProductResponse mapToResponse(Product product, boolean nameWarning, String warningMessage) {
        return ProductResponse.builder()
                .id(product.getId())
                .maSo(product.getMaSo())
                .tenHang(product.getTenHang())
                .nhomHang(product.getProductGroup() != null ? product.getProductGroup().getCode() : null)
                .nhomHangTen(product.getProductGroup() != null ? product.getProductGroup().getName() : null)
                .donViTinh(product.getUnit() != null ? product.getUnit().getName() : null)
                .donGiaNhap(product.getDonGiaNhap())
                .trangThai(product.getTrangThai())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .nameWarning(nameWarning)
                .warningMessage(warningMessage)
                .build();
    }
}
