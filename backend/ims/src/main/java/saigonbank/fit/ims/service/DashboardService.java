package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.response.DashboardResponse;
import saigonbank.fit.ims.repository.InventoryRepository;
import saigonbank.fit.ims.repository.InventoryTransactionRepository;
import saigonbank.fit.ims.repository.ProductRepository;
import saigonbank.fit.ims.entity.InventoryTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear  = now.getYear();

        long totalActive = productRepository.countActive();
        long withStock = inventoryRepository.countProductsWithStock();
        BigDecimal totalAmount = inventoryRepository.sumTotalAmount();
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        long importsThisMonth = transactionRepository.countByTypeAndTransactionDateBetween(InventoryTransaction.TransactionType.IMPORT, startOfMonth, endOfMonth);
        long exportsThisMonth = transactionRepository.countByTypeAndTransactionDateBetween(InventoryTransaction.TransactionType.EXPORT, startOfMonth, endOfMonth);

        return DashboardResponse.builder()
                .totalActiveProducts(totalActive)
                .productsWithStock(withStock)
                .totalInventoryAmount(totalAmount)
                .importsThisMonth(importsThisMonth)
                .exportsThisMonth(exportsThisMonth)
                .currentMonth(currentMonth)
                .currentYear(currentYear)
                .build();
    }
}
