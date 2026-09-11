package saigonbank.fit.ims.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.Inventory;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Inventory i WHERE i.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

    @Query(
        value = """
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH p.productGroup pg
            JOIN FETCH p.unit u
            WHERE (:maSo IS NULL OR LOWER(p.maSo) LIKE LOWER(CONCAT('%', :maSo, '%')))
              AND (:tenHang IS NULL OR LOWER(p.tenHang) LIKE LOWER(CONCAT('%', :tenHang, '%')))
              AND (:nhomHang IS NULL OR pg.code = :nhomHang)
              AND p.trangThai = 'HOAT_DONG'
            ORDER BY p.maSo ASC
        """,
        countQuery = """
            SELECT COUNT(i) FROM Inventory i
            JOIN i.product p
            JOIN p.productGroup pg
            WHERE (:maSo IS NULL OR LOWER(p.maSo) LIKE LOWER(CONCAT('%', :maSo, '%')))
              AND (:tenHang IS NULL OR LOWER(p.tenHang) LIKE LOWER(CONCAT('%', :tenHang, '%')))
              AND (:nhomHang IS NULL OR pg.code = :nhomHang)
              AND p.trangThai = 'HOAT_DONG'
        """
    )
    Page<Inventory> findWithFilters(
            @Param("maSo") String maSo,
            @Param("tenHang") String tenHang,
            @Param("nhomHang") String nhomHang,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(i.endingAmount), 0) FROM Inventory i WHERE i.product.trangThai = 'HOAT_DONG'")
    java.math.BigDecimal sumTotalAmount();

    @Query("""
        SELECT COUNT(i) FROM Inventory i
        WHERE i.endingQuantity > 0 AND i.product.trangThai = 'HOAT_DONG'
    """)
    long countProductsWithStock();
}
