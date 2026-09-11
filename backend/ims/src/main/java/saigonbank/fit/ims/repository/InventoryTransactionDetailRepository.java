package saigonbank.fit.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.InventoryTransactionDetail;

import java.util.List;

@Repository
public interface InventoryTransactionDetailRepository extends JpaRepository<InventoryTransactionDetail, Long> {

    List<InventoryTransactionDetail> findByTransactionId(Long transactionId);
    List<InventoryTransactionDetail> findByProductId(Long productId);
    boolean existsByProductId(Long productId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM InventoryTransactionDetail d JOIN FETCH d.transaction t WHERE d.product.id = :productId ORDER BY t.transactionDate ASC, d.id ASC")
    List<InventoryTransactionDetail> findByProductIdWithTransaction(@org.springframework.data.repository.query.Param("productId") Long productId);
}
