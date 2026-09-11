package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.request.ImportDetailRequest;
import saigonbank.fit.ims.dto.request.ImportRequest;
import saigonbank.fit.ims.dto.response.ImportDetailResponse;
import saigonbank.fit.ims.dto.response.ImportResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.entity.*;
import saigonbank.fit.ims.exception.AcqtMixingException;
import saigonbank.fit.ims.exception.InvalidImportException;
import saigonbank.fit.ims.exception.MonthLockedException;
import saigonbank.fit.ims.exception.ProductNotFoundException;
import saigonbank.fit.ims.exception.ResourceNotFoundException;
import saigonbank.fit.ims.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ImportService — tạo, hủy, tra cứu phiếu nhập hàng.
 *
 * Quy trình tạo phiếu nhập:
 * 1. Kiểm tra tháng không bị khóa
 * 2. Kiểm tra quy tắc ACQT (ACQT lập phiếu riêng, không gộp chung nhóm khác)
 * 3. Xác định nhóm hàng đại diện (nhiều dòng hơn thắng, bằng nhau: ACTT > VPP > VLK)
 * 4. Cấp số phiếu (pessimistic lock trên document_sequences)
 * 5. Tạo InventoryTransaction + InventoryTransactionDetail
 * 6. Cập nhật Inventory (tồn kho hiện tại)
 * 7. Cập nhật Kkts (tồn kho theo kỳ)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final KktsRepository kktsRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final MonthlyClosingService monthlyClosingService;
    private final AuditLogService auditLogService;

    // Độ ưu tiên nhóm đại diện khi số dòng bằng nhau (ACTT > VPP > VLK)
    private static final Map<String, Integer> GROUP_PRIORITY = Map.of(
            "ACTT", 1,
            "VPP",  2,
            "VLK",  3
    );

    @Transactional
    public ImportResponse createImport(ImportRequest request, String username) {
        checkMonthNotLocked(request.getNgayNhap());

        List<LineItem> lineItems = resolveLineItems(request.getDetails());
        validateAcqtMixing(lineItems);
        String groupCode = determineGroupCode(lineItems);

        int year = request.getNgayNhap().getYear();
        String transactionCode = generateTransactionCode(year, groupCode, "IMPORT");

        BigDecimal totalAmount = lineItems.stream()
                .map(li -> li.amount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        InventoryTransaction tx = InventoryTransaction.builder()
                .transactionCode(transactionCode)
                .type(InventoryTransaction.TransactionType.IMPORT)
                .transactionDate(request.getNgayNhap())
                .destination(request.getNoiNhap())
                .source(request.getNoiMua())
                .invoiceNumber(request.getSoHoaDon())
                .invoiceDate(request.getNgayHoaDon())
                .debitAccount(request.getTkNo())
                .creditAccount(request.getTkCo())
                .totalAmount(totalAmount)
                .groupRepresentative(groupCode)
                .createdBy(username)
                .build();

        List<InventoryTransactionDetail> details = new ArrayList<>();
        for (LineItem li : lineItems) {
            InventoryTransactionDetail detail = InventoryTransactionDetail.builder()
                    .transaction(tx)
                    .product(li.product())
                    .productGroupCode(li.product().getNhomHangCode())
                    .quantity(li.quantity())
                    .unitPrice(li.unitPrice())
                    .amount(li.amount())
                    .build();
            details.add(detail);
        }
        tx.getDetails().addAll(details);
        tx = transactionRepository.save(tx);

        LocalDate txDate = request.getNgayNhap();
        for (LineItem li : lineItems) {
            updateInventoryImport(li.product(), li.quantity(), li.amount());
            updateKktsImport(li.product(), txDate.getYear(), txDate.getMonthValue(), li.quantity(), li.amount());
        }

        log.info("Created import {} for group {} by {}", transactionCode, groupCode, username);
        auditLogService.logAction(username, "CREATE", "IMPORT", transactionCode,
                "Tạo phiếu nhập " + transactionCode + " — Tổng tiền: " + totalAmount);
        return mapToImportResponse(tx);
    }

    @Transactional
    public ImportResponse cancelImport(Long id, String username) {
        InventoryTransaction tx = findImportById(id);

        if (InventoryTransaction.TransactionStatus.HUY.equals(tx.getStatus())) {
            throw new InvalidImportException("Phiếu nhập " + tx.getTransactionCode() + " đã bị hủy trước đó.");
        }

        checkMonthNotLocked(tx.getTransactionDate());

        for (InventoryTransactionDetail detail : tx.getDetails()) {
            reverseInventoryImport(detail.getProduct(), detail.getQuantity(), detail.getAmount());
            reverseKktsImport(detail.getProduct(),
                    tx.getTransactionDate().getYear(),
                    tx.getTransactionDate().getMonthValue(),
                    detail.getQuantity(), detail.getAmount());
        }

        tx.setStatus(InventoryTransaction.TransactionStatus.HUY);
        tx = transactionRepository.save(tx);

        log.info("Cancelled import {} by {}", tx.getTransactionCode(), username);
        auditLogService.logAction(username, "CANCEL", "IMPORT", tx.getTransactionCode(),
                "Hủy phiếu nhập " + tx.getTransactionCode());
        return mapToImportResponse(tx);
    }

    /**
     * Lấy danh sách phiếu nhập với filter + phân trang.
     */
    @Transactional(readOnly = true)
    public PageResponse<ImportResponse> getImports(String soPhieu, String noiMua,
                                                    String fromDate, String toDate,
                                                    String trangThai, Pageable pageable) {
        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to   = toDate   != null ? LocalDate.parse(toDate)   : null;

        Page<InventoryTransaction> page =
                transactionRepository.findImports(soPhieu, noiMua, trangThai, from, to, pageable);

        return PageResponse.<ImportResponse>builder()
                .content(page.getContent().stream().map(this::mapToImportResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    /**
     * Lấy chi tiết phiếu nhập theo ID.
     */
    @Transactional(readOnly = true)
    public ImportResponse getImportById(Long id) {
        return mapToImportResponse(findImportById(id));
    }

    private InventoryTransaction findImportById(Long id) {
        InventoryTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập với ID: " + id));
        if (tx.getType() != InventoryTransaction.TransactionType.IMPORT) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu nhập với ID: " + id);
        }
        return tx;
    }

    private void checkMonthNotLocked(LocalDate date) {
        if (monthlyClosingService.isLocked(date.getYear(), date.getMonthValue())) {
            throw new MonthLockedException(date.getMonthValue(), date.getYear());
        }
    }

    private record LineItem(Product product, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {}

    private List<LineItem> resolveLineItems(List<ImportDetailRequest> details) {
        List<LineItem> items = new ArrayList<>();
        for (ImportDetailRequest d : details) {
            Product product = productRepository.findById(d.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm ID: " + d.getProductId()));
            if (!product.isActive()) {
                throw new InvalidImportException("Sản phẩm " + product.getMaSo() + " đã ngừng sử dụng.");
            }
            BigDecimal amount = d.getQuantity().multiply(d.getUnitPrice());
            items.add(new LineItem(product, d.getQuantity(), d.getUnitPrice(), amount));
        }
        return items;
    }

    // Xác định nhóm đại diện: nhiều dòng hơn thắng, nếu bằng nhau thì ưu tiên theo GROUP_PRIORITY (ACTT > VPP > VLK)
    private String determineGroupCode(List<LineItem> lineItems) {
        Map<String, Long> countByGroup = lineItems.stream()
                .collect(Collectors.groupingBy(li -> li.product().getNhomHangCode(), Collectors.counting()));

        List<String> groups = new ArrayList<>(countByGroup.keySet());

        if (groups.size() == 1) {
            return groups.get(0);
        }

        return groups.stream()
                .max(Comparator
                        .comparingLong((String g) -> countByGroup.getOrDefault(g, 0L))
                        .thenComparingInt(g -> -(GROUP_PRIORITY.getOrDefault(g, 99)))
                )
                .orElseThrow(() -> new InvalidImportException("Không thể xác định nhóm hàng đại diện."));
    }

    // ACQT phải lập phiếu riêng, không được gộp chung nhóm khác
    private void validateAcqtMixing(List<LineItem> lineItems) {
        boolean hasAcqt = lineItems.stream().anyMatch(li -> "ACQT".equals(li.product().getNhomHangCode()));
        boolean hasOther = lineItems.stream().anyMatch(li -> !"ACQT".equals(li.product().getNhomHangCode()));

        if (hasAcqt && hasOther) {
            throw new AcqtMixingException("ACQT phải lập phiếu nhập riêng, không được gộp chung với nhóm hàng khác.");
        }
    }

    // Cấp số phiếu STT/GROUP-YYYY với INSERT IGNORE và pessimistic lock
    private String generateTransactionCode(int year, String groupCode, String docType) {
        documentSequenceRepository.insertIgnore(year, groupCode, docType);

        DocumentSequence seq = documentSequenceRepository
                .findForUpdate(year, groupCode, docType)
                .orElseThrow(() -> new InvalidImportException(
                        "Không thể tạo sequence cho " + groupCode + "-" + year));

        int nextSeq = seq.nextSequence();
        documentSequenceRepository.save(seq);
        return nextSeq + "/" + groupCode + "-" + year;
    }

    private void updateInventoryImport(Product product, BigDecimal qty, BigDecimal amount) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho sản phẩm: " + product.getMaSo()));
        inventory.applyImport(qty, amount);
        inventoryRepository.save(inventory);
    }

    private void reverseInventoryImport(Product product, BigDecimal qty, BigDecimal amount) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho sản phẩm: " + product.getMaSo()));
        inventory.reverseImport(qty, amount);
        inventoryRepository.save(inventory);
    }

    private void updateKktsImport(Product product, int year, int month, BigDecimal qty, BigDecimal amount) {
        Kkts kkts = kktsRepository.findByProductIdAndYearAndMonth(product.getId(), year, month)
                .orElseGet(() -> {
                    // Lấy tồn cuối tháng trước làm tồn đầu tháng mới
                    java.time.LocalDate prevMonthDate = java.time.LocalDate.of(year, month, 1).minusMonths(1);
                    int prevYear  = prevMonthDate.getYear();
                    int prevMonth = prevMonthDate.getMonthValue();

                    BigDecimal openingQty = BigDecimal.ZERO;
                    BigDecimal openingAmt = BigDecimal.ZERO;
                    java.util.Optional<Kkts> prevKkts = kktsRepository
                            .findByProductIdAndYearAndMonth(product.getId(), prevYear, prevMonth);
                    if (prevKkts.isPresent()) {
                        openingQty = prevKkts.get().getEndingQuantity();
                        openingAmt = prevKkts.get().getEndingAmount();
                    }

                    Kkts newKkts = Kkts.builder()
                            .product(product)
                            .year(year)
                            .month(month)
                            .openingQuantity(openingQty)
                            .openingAmount(openingAmt)
                            .build();
                    newKkts.recalculate();
                    return kktsRepository.save(newKkts);
                });
        kkts.applyImport(qty, amount);
        kktsRepository.save(kkts);
    }


    private void reverseKktsImport(Product product, int year, int month, BigDecimal qty, BigDecimal amount) {
        kktsRepository.findByProductIdAndYearAndMonth(product.getId(), year, month)
                .ifPresent(kkts -> {
                    kkts.reverseImport(qty, amount);
                    kktsRepository.save(kkts);
                });
    }

    private ImportResponse mapToImportResponse(InventoryTransaction tx) {
        List<ImportDetailResponse> details = tx.getDetails().stream()
                .map(d -> ImportDetailResponse.builder()
                        .id(d.getId())
                        .productId(d.getProduct().getId())
                        .maSo(d.getProduct().getMaSo())
                        .tenHang(d.getProduct().getTenHang())
                        .nhomHang(d.getProductGroupCode())
                        .donViTinh(d.getProduct().getDonViTinh())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .amount(d.getAmount())
                        .build())
                .toList();

        return ImportResponse.builder()
                .id(tx.getId())
                .soPhieu(tx.getTransactionCode())
                .ngayNhap(tx.getTransactionDate())
                .noiNhap(tx.getDestination())
                .noiMua(tx.getSource())
                .soHoaDon(tx.getInvoiceNumber())
                .ngayHoaDon(tx.getInvoiceDate())
                .tkNo(tx.getDebitAccount())
                .tkCo(tx.getCreditAccount())
                .tongTien(tx.getTotalAmount())
                .trangThai(tx.getStatus())
                .nhomDaiDien(tx.getGroupRepresentative())
                .workflowStatus(tx.getWorkflowStatus())
                .createdBy(tx.getCreatedBy())
                .createdAt(tx.getCreatedAt())
                .details(details)
                .build();
    }
}
