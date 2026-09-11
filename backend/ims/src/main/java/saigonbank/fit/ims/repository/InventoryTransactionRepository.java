package saigonbank.fit.ims.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.InventoryTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    Optional<InventoryTransaction> findByTransactionCode(String transactionCode);

    List<InventoryTransaction> findByTypeAndTransactionDateBetween(
            InventoryTransaction.TransactionType type, LocalDate startDate, LocalDate endDate);

    long countByTypeAndTransactionDateBetween(
            InventoryTransaction.TransactionType type, LocalDate startDate, LocalDate endDate);

    // Danh sách phiếu nhập kèm filter và phân trang
    @Query(
        value = """
            SELECT t FROM InventoryTransaction t
            WHERE t.type = 'IMPORT'
              AND (:soPhieu  IS NULL OR LOWER(t.transactionCode) LIKE LOWER(CONCAT('%', :soPhieu, '%')))
              AND (:noiMua   IS NULL OR LOWER(t.source)          LIKE LOWER(CONCAT('%', :noiMua, '%')))
              AND (:trangThai IS NULL OR t.status = :trangThai)
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate   IS NULL OR t.transactionDate <= :toDate)
            ORDER BY t.transactionDate DESC, t.id DESC
        """,
        countQuery = """
            SELECT COUNT(t) FROM InventoryTransaction t
            WHERE t.type = 'IMPORT'
              AND (:soPhieu   IS NULL OR LOWER(t.transactionCode) LIKE LOWER(CONCAT('%', :soPhieu, '%')))
              AND (:noiMua    IS NULL OR LOWER(t.source)          LIKE LOWER(CONCAT('%', :noiMua, '%')))
              AND (:trangThai IS NULL OR t.status = :trangThai)
              AND (:fromDate  IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate    IS NULL OR t.transactionDate <= :toDate)
        """
    )
    Page<InventoryTransaction> findImports(
            @Param("soPhieu")   String soPhieu,
            @Param("noiMua")    String noiMua,
            @Param("trangThai") String trangThai,
            @Param("fromDate")  LocalDate fromDate,
            @Param("toDate")    LocalDate toDate,
            Pageable pageable
    );

    // Danh sách phiếu xuất kèm filter và phân trang
    @Query(
        value = """
            SELECT t FROM InventoryTransaction t
            WHERE t.type = 'EXPORT'
              AND (:soPhieu   IS NULL OR LOWER(t.transactionCode) LIKE LOWER(CONCAT('%', :soPhieu, '%')))
              AND (:noiNhan   IS NULL OR LOWER(t.destination)     LIKE LOWER(CONCAT('%', :noiNhan, '%')))
              AND (:trangThai IS NULL OR t.status = :trangThai)
              AND (:fromDate  IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate    IS NULL OR t.transactionDate <= :toDate)
            ORDER BY t.transactionDate DESC, t.id DESC
        """,
        countQuery = """
            SELECT COUNT(t) FROM InventoryTransaction t
            WHERE t.type = 'EXPORT'
              AND (:soPhieu   IS NULL OR LOWER(t.transactionCode) LIKE LOWER(CONCAT('%', :soPhieu, '%')))
              AND (:noiNhan   IS NULL OR LOWER(t.destination)     LIKE LOWER(CONCAT('%', :noiNhan, '%')))
              AND (:trangThai IS NULL OR t.status = :trangThai)
              AND (:fromDate  IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate    IS NULL OR t.transactionDate <= :toDate)
        """
    )
    Page<InventoryTransaction> findExports(
            @Param("soPhieu")   String soPhieu,
            @Param("noiNhan")   String noiNhan,
            @Param("trangThai") String trangThai,
            @Param("fromDate")  LocalDate fromDate,
            @Param("toDate")    LocalDate toDate,
            Pageable pageable
    );
}
