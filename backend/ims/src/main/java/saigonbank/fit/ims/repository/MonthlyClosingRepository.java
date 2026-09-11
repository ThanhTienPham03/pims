package saigonbank.fit.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.MonthlyClosing;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyClosingRepository extends JpaRepository<MonthlyClosing, Long> {

    Optional<MonthlyClosing> findByYearAndMonth(Integer year, Integer month);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END FROM MonthlyClosing m WHERE m.year = :year AND m.month = :month AND m.isLocked = TRUE")
    boolean isLocked(@Param("year") Integer year, @Param("month") Integer month);

    List<MonthlyClosing> findAllByOrderByYearDescMonthDesc();
}
