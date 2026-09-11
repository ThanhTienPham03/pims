package saigonbank.fit.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.Kkts;

import java.util.List;
import java.util.Optional;

@Repository
public interface KktsRepository extends JpaRepository<Kkts, Long> {

    Optional<Kkts> findByProductIdAndYearAndMonth(Long productId, Integer year, Integer month);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Kkts k WHERE k.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Query("""
        SELECT k FROM Kkts k
        JOIN FETCH k.product p
        JOIN FETCH p.productGroup pg
        JOIN FETCH p.unit u
        WHERE k.year = :year AND k.month = :month
        ORDER BY p.maSo ASC
    """)
    List<Kkts> findByPeriod(@Param("year") Integer year, @Param("month") Integer month);

    @Query("""
        SELECT k FROM Kkts k
        WHERE k.product.id = :productId AND k.year = :year AND k.month = :month
    """)
    Optional<Kkts> findByProductAndPeriod(
            @Param("productId") Long productId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
