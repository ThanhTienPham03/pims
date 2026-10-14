package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.response.InventoryResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.dto.response.StockCardResponse;
import saigonbank.fit.ims.entity.Inventory;
import saigonbank.fit.ims.entity.Product;
import saigonbank.fit.ims.exception.ResourceNotFoundException;
import saigonbank.fit.ims.repository.InventoryRepository;
import saigonbank.fit.ims.repository.ProductRepository;


import saigonbank.fit.ims.dto.response.StockCardEntryResponse;
import saigonbank.fit.ims.entity.InventoryTransaction;
import saigonbank.fit.ims.entity.InventoryTransactionDetail;
import saigonbank.fit.ims.repository.InventoryTransactionDetailRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionDetailRepository transactionDetailRepository;

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventoryList(String maSo, String tenHang,
                                                            String nhomHang, Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findWithFilters(maSo, tenHang, nhomHang, pageable);
        List<InventoryResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<InventoryResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProduct(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tồn kho cho sản phẩm: " + productId));
        return mapToResponse(inventory);
    }

    @Transactional(readOnly = true)
    public StockCardResponse getStockCard(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);
        BigDecimal runningQty = inventory != null ? inventory.getOpeningQuantity() : BigDecimal.ZERO;
        BigDecimal runningAmt = inventory != null ? inventory.getOpeningAmount() : BigDecimal.ZERO;

        List<StockCardEntryResponse> entries = new ArrayList<>();

        entries.add(StockCardEntryResponse.builder()
                .tranType("OPEN")
                .soPhieu("OPEN")
                .note("Số dư đầu kỳ")
                .balanceQuantity(runningQty)
                .balanceAmount(runningAmt)
                .build());

        List<InventoryTransactionDetail> details = transactionDetailRepository.findByProductIdWithTransaction(productId);
        for (InventoryTransactionDetail detail : details) {
            InventoryTransaction tx = detail.getTransaction();
            boolean isImport = tx.getType() == InventoryTransaction.TransactionType.IMPORT;
            BigDecimal qty = detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ZERO;
            BigDecimal amt = detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO;

            if (isImport) {
                runningQty = runningQty.add(qty);
                runningAmt = runningAmt.add(amt);
                entries.add(StockCardEntryResponse.builder()
                        .id(detail.getId())
                        .entryDate(tx.getTransactionDate())
                        .soPhieu(tx.getTransactionCode())
                        .tranType(tx.getType().name())
                        .importQuantity(qty)
                        .importUnitPrice(detail.getUnitPrice())
                        .importAmount(amt)
                        .balanceQuantity(runningQty)
                        .balanceAmount(runningAmt)
                        .createdBy(tx.getCreatedBy())
                        .build());
            } else {
                runningQty = runningQty.subtract(qty);
                runningAmt = runningAmt.subtract(amt);
                entries.add(StockCardEntryResponse.builder()
                        .id(detail.getId())
                        .entryDate(tx.getTransactionDate())
                        .soPhieu(tx.getTransactionCode())
                        .tranType(tx.getType().name())
                        .exportQuantity(qty)
                        .exportUnitPrice(detail.getUnitPrice())
                        .exportAmount(amt)
                        .balanceQuantity(runningQty)
                        .balanceAmount(runningAmt)
                        .note(tx.getReason())
                        .createdBy(tx.getCreatedBy())
                        .build());
            }
        }

        return StockCardResponse.builder()
                .productId(product.getId())
                .maSo(product.getMaSo())
                .tenHang(product.getTenHang())
                .nhomHang(product.getNhomHangCode())
                .donViTinh(product.getDonViTinh())
                .entries(entries)
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getInventorySummary() {
        long totalProducts = productRepository.countActive();
        long withStock = inventoryRepository.countProductsWithStock();
        BigDecimal totalAmount = inventoryRepository.sumTotalAmount();
        return java.util.Map.of(
                "totalProducts", totalProducts,
                "productsWithStock", withStock,
                "totalInventoryAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO
        );
    }

    private InventoryResponse mapToResponse(Inventory i) {
        Product p = i.getProduct();
        return InventoryResponse.builder()
                .inventoryId(i.getId())
                .productId(p.getId())
                .maSo(p.getMaSo())
                .tenHang(p.getTenHang())
                .nhomHang(p.getProductGroup() != null ? p.getProductGroup().getCode() : null)
                .nhomHangTen(p.getProductGroup() != null ? p.getProductGroup().getName() : null)
                .donViTinh(p.getUnit() != null ? p.getUnit().getName() : null)
                .trangThai(p.getTrangThai())
                .openingQuantity(i.getOpeningQuantity())
                .openingAmount(i.getOpeningAmount())
                .importQuantity(i.getImportQuantity())
                .importAmount(i.getImportAmount())
                .exportQuantity(i.getExportQuantity())
                .exportAmount(i.getExportAmount())
                .endingQuantity(i.getEndingQuantity())
                .endingAmount(i.getEndingAmount())
                .averagePrice(i.getAveragePrice())
                .build();
    }
}
