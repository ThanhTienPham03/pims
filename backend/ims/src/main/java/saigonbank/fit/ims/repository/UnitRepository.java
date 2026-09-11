package saigonbank.fit.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saigonbank.fit.ims.entity.Unit;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByCode(String code);
}
