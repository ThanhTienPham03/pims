package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.request.ExportDetailRequest;
import saigonbank.fit.ims.dto.request.ExportRequest;
import saigonbank.fit.ims.dto.response.ExportDetailResponse;
import saigonbank.fit.ims.dto.response.ExportResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.entity.*;
import saigonbank.fit.ims.entity.User;
import saigonbank.fit.ims.exception.*;
import saigonbank.fit.ims.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final KktsRepository kktsRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final MonthlyClosingService monthlyClosingService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    // Độ ưu tiên nhóm đại diện khi số dòng bằng nhau (ACTT > VPP > VLK)
    private static final Map<String, Integer> GROUP_PRIORITY = Map.of(
            "ACTT", 1,
            "VPP", 2,
            "VLK", 3);

    @Transactional
    public ExportResponse createExport(ExportRequest request, String username) {
        checkMonthNotLocked(request.getNgayXuat());

        List<LineItem> lineItems = resolveLineItems(request.getDetails(), request.getNgayXuat());
        validateAcqtMixing(lineItems);
        String groupCode = determineGroupCode(lineItems);

        int year = request.getNgayXuat().getYear();
        String transactionCode = generateTransactionCode(year, groupCode, "EXPORT");

        BigDecimal totalAmount = lineItems.stream()
                .map(li -> li.amount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        InventoryTransaction tx = InventoryTransaction.builder()
                .transactionCode(transactionCode)
                .type(InventoryTransaction.TransactionType.EXPORT)
                .transactionDate(request.getNgayXuat())
                .destination(request.getNoiNhan())
                .reason(request.getLyDoXuat())
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

        LocalDate txDate = request.getNgayXuat();
        for (LineItem li : lineItems) {
            updateInventoryExport(li.product(), li.quantity(), li.amount());
            updateKktsExport(li.product(), txDate.getYear(), txDate.getMonthValue(), li.quantity(), li.amount());
        }

        log.info("Created export {} for group {} by {}", transactionCode, groupCode, username);
        auditLogService.logAction(username, "CREATE", "EXPORT", transactionCode,
                "Tạo phiếu xuất " + transactionCode + " — Tổng tiền: " + totalAmount);
        return mapToExportResponse(tx);
    }

    @Transactional
    public ExportResponse cancelExport(Long id, String username) {
        InventoryTransaction tx = findExportById(id);

        if (InventoryTransaction.TransactionStatus.HUY.equals(tx.getStatus())) {
            throw new InvalidExportException("Phiếu xuất " + tx.getTransactionCode() + " đã bị hủy trước đó.");
        }

        checkMonthNotLocked(tx.getTransactionDate());

        for (InventoryTransactionDetail detail : tx.getDetails()) {
            reverseInventoryExport(detail.getProduct(), detail.getQuantity(), detail.getAmount());
            reverseKktsExport(detail.getProduct(),
                    tx.getTransactionDate().getYear(),
                    tx.getTransactionDate().getMonthValue(),
                    detail.getQuantity(), detail.getAmount());
        }

        tx.setStatus(InventoryTransaction.TransactionStatus.HUY);
        tx = transactionRepository.save(tx);

        log.info("Cancelled export {} by {}", tx.getTransactionCode(), username);
        auditLogService.logAction(username, "CANCEL", "EXPORT", tx.getTransactionCode(),
                "Hủy phiếu xuất " + tx.getTransactionCode());
        return mapToExportResponse(tx);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExportResponse> getExports(String soPhieu, String noiNhan,
            String fromDate, String toDate,
            String trangThai, Pageable pageable) {
        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        Page<InventoryTransaction> page = transactionRepository.findExports(soPhieu, noiNhan, trangThai, from, to,
                pageable);

        List<String> usernames = page.getContent().stream()
                .map(InventoryTransaction::getCreatedBy)
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .toList();

        Map<String, String> userFullNameMap = userRepository.findByUsernameIn(usernames).stream()
                .filter(u -> u.getUsername() != null && u.getFullName() != null && !u.getFullName().isBlank())
                .collect(Collectors.toMap(
                        User::getUsername,
                        User::getFullName,
                        (existing, replacement) -> existing));

        return PageResponse.<ExportResponse>builder()
                .content(page.getContent().stream()
                        .map(tx -> mapToExportResponse(tx,
                                userFullNameMap.getOrDefault(tx.getCreatedBy(), tx.getCreatedBy())))
                        .toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ExportResponse getExportById(Long id) {
        return mapToExportResponse(findExportById(id));
    }

    private InventoryTransaction findExportById(Long id) {
        InventoryTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu xuất với ID: " + id));
        if (tx.getType() != InventoryTransaction.TransactionType.EXPORT) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu xuất với ID: " + id);
        }
        return tx;
    }

    private void checkMonthNotLocked(LocalDate date) {
        if (monthlyClosingService.isLocked(date.getYear(), date.getMonthValue())) {
            throw new MonthLockedException(date.getMonthValue(), date.getYear());
        }
    }

    private record LineItem(Product product, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    // Đơn giá xuất lấy từ KKTS tháng trước (fallback: giá TB tồn kho); nếu xuất hết
    // thì dùng endingAmount
    private List<LineItem> resolveLineItems(List<ExportDetailRequest> details, LocalDate exportDate) {
        LocalDate prevMonthDate = exportDate.minusMonths(1);
        int prevYear = prevMonthDate.getYear();
        int prevMonth = prevMonthDate.getMonthValue();

        List<LineItem> items = new ArrayList<>();
        for (ExportDetailRequest d : details) {
            Product product = productRepository.findById(d.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm ID: " + d.getProductId()));

            Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy tồn kho cho sản phẩm: " + product.getMaSo()));

            if (inventory.getEndingQuantity().compareTo(d.getQuantity()) < 0) {
                throw new InsufficientStockException("Sản phẩm " + product.getMaSo()
                        + " không đủ tồn kho. Tồn hiện tại: " + inventory.getEndingQuantity());
            }

            BigDecimal unitPrice = kktsRepository
                    .findByProductIdAndYearAndMonth(product.getId(), prevYear, prevMonth)
                    .map(Kkts::getAveragePrice)
                    .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                    .orElse(inventory.getAveragePrice());

            BigDecimal amount;
            if (inventory.getEndingQuantity().compareTo(d.getQuantity()) == 0) {
                amount = inventory.getEndingAmount();
            } else {
                amount = d.getQuantity().multiply(unitPrice);
            }
            items.add(new LineItem(product, d.getQuantity(), unitPrice, amount));
        }
        return items;
    }

    // Xác định nhóm đại diện: nhiều dòng hơn thắng, nếu bằng nhau thì ưu tiên theo
    // GROUP_PRIORITY
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
                        .thenComparingInt(g -> -(GROUP_PRIORITY.getOrDefault(g, 99))))
                .orElseThrow(() -> new InvalidExportException("Không thể xác định nhóm hàng đại diện."));
    }

    // ACQT phải lập phiếu riêng, không được gộp chung nhóm khác
    private void validateAcqtMixing(List<LineItem> lineItems) {
        boolean hasAcqt = lineItems.stream().anyMatch(li -> "ACQT".equals(li.product().getNhomHangCode()));
        boolean hasOther = lineItems.stream().anyMatch(li -> !"ACQT".equals(li.product().getNhomHangCode()));

        if (hasAcqt && hasOther) {
            throw new AcqtMixingException("ACQT phải lập phiếu xuất riêng, không được gộp chung với nhóm hàng khác.");
        }
    }

    // Cấp số phiếu STT/GROUP-YYYY với INSERT IGNORE và pessimistic lock
    private String generateTransactionCode(int year, String groupCode, String docType) {
        documentSequenceRepository.insertIgnore(year, groupCode, docType);

        DocumentSequence seq = documentSequenceRepository
                .findForUpdate(year, groupCode, docType)
                .orElseThrow(() -> new InvalidExportException(
                        "Không thể tạo sequence cho " + groupCode + "-" + year));

        int nextSeq = seq.nextSequence();
        documentSequenceRepository.save(seq);
        return nextSeq + "/" + groupCode + "-" + year;
    }

    private void updateInventoryExport(Product product, BigDecimal qty, BigDecimal amount) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho sản phẩm: " + product.getMaSo()));
        inventory.applyExport(qty, amount);
        inventoryRepository.save(inventory);
    }

    private void reverseInventoryExport(Product product, BigDecimal qty, BigDecimal amount) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho sản phẩm: " + product.getMaSo()));
        inventory.reverseExport(qty, amount);
        inventoryRepository.save(inventory);
    }

    private void updateKktsExport(Product product, int year, int month, BigDecimal qty, BigDecimal amount) {
        Kkts kkts = kktsRepository.findByProductIdAndYearAndMonth(product.getId(), year, month)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy KKTS cho tháng " + month + "/" + year));
        kkts.applyExport(qty, amount);
        kktsRepository.save(kkts);
    }

    private void reverseKktsExport(Product product, int year, int month, BigDecimal qty, BigDecimal amount) {
        kktsRepository.findByProductIdAndYearAndMonth(product.getId(), year, month)
                .ifPresent(kkts -> {
                    kkts.reverseExport(qty, amount);
                    kktsRepository.save(kkts);
                });
    }

    private String resolveFullName(String username) {
        if (username == null || username.isBlank())
            return null;
        return userRepository.findByUsername(username)
                .map(User::getFullName)
                .filter(name -> !name.isBlank())
                .orElse(username);
    }

    private ExportResponse mapToExportResponse(InventoryTransaction tx) {
        return mapToExportResponse(tx, resolveFullName(tx.getCreatedBy()));
    }

    private ExportResponse mapToExportResponse(InventoryTransaction tx, String creatorFullName) {
        List<ExportDetailResponse> details = tx.getDetails().stream()
                .map(d -> ExportDetailResponse.builder()
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

        String resolvedCreator = (creatorFullName != null && !creatorFullName.isBlank()) ? creatorFullName
                : tx.getCreatedBy();

        return ExportResponse.builder()
                .id(tx.getId())
                .soPhieu(tx.getTransactionCode())
                .ngayXuat(tx.getTransactionDate())
                .noiNhan(tx.getDestination())
                .lyDoXuat(tx.getReason())
                .tkNo(tx.getDebitAccount())
                .tkCo(tx.getCreditAccount())
                .tongTien(tx.getTotalAmount())
                .trangThai(tx.getStatus())
                .nhomDaiDien(tx.getGroupRepresentative())
                .workflowStatus(tx.getWorkflowStatus())
                .createdBy(tx.getCreatedBy())
                .creatorFullName(resolvedCreator)
                .createdAt(tx.getCreatedAt())
                .details(details)
                .build();
    }
}
