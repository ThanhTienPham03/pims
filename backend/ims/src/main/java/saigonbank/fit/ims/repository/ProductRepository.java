package saigonbank.fit.ims.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByMaSo(String maSo);

    boolean existsByMaSo(String maSo);

    boolean existsByMaSoAndIdNot(String maSo, Long id);

    @Query(
        value = """
            SELECT p FROM Product p
            JOIN FETCH p.productGroup pg
            JOIN FETCH p.unit u
            WHERE (:maSo IS NULL OR LOWER(p.maSo) LIKE LOWER(CONCAT('%', :maSo, '%')))
              AND (:tenHang IS NULL OR LOWER(p.tenHang) LIKE LOWER(CONCAT('%', :tenHang, '%')))
              AND (:nhomHang IS NULL OR pg.code = :nhomHang)
              AND (:trangThai IS NULL OR p.trangThai = :trangThai)
            ORDER BY p.maSo ASC
        """,
        countQuery = """
            SELECT COUNT(p) FROM Product p
            JOIN p.productGroup pg
            WHERE (:maSo IS NULL OR LOWER(p.maSo) LIKE LOWER(CONCAT('%', :maSo, '%')))
              AND (:tenHang IS NULL OR LOWER(p.tenHang) LIKE LOWER(CONCAT('%', :tenHang, '%')))
              AND (:nhomHang IS NULL OR pg.code = :nhomHang)
              AND (:trangThai IS NULL OR p.trangThai = :trangThai)
        """
    )
    Page<Product> findWithFilters(
            @Param("maSo") String maSo,
            @Param("tenHang") String tenHang,
            @Param("nhomHang") String nhomHang,
            @Param("trangThai") String trangThai,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.productGroup pg
        JOIN FETCH p.unit u
        WHERE p.trangThai = 'HOAT_DONG'
        ORDER BY p.tenHang ASC
    """)
    List<Product> findAllActive();

    /**
     * Find products with similar names for duplicate warning.
     */
    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.tenHang) LIKE LOWER(CONCAT('%', :tenHang, '%'))
          AND p.id != :excludeId
    """)
    List<Product> findSimilarName(@Param("tenHang") String tenHang, @Param("excludeId") Long excludeId);



    @Query("SELECT COUNT(p) FROM Product p WHERE p.trangThai = 'HOAT_DONG'")
    long countActive();

    @Query("SELECT p.maSo FROM Product p WHERE UPPER(p.maSo) LIKE CONCAT(UPPER(:prefix), '%')")
    List<String> findMaSoByPrefix(@Param("prefix") String prefix);
}
