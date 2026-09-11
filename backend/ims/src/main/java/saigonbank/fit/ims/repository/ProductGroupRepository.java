package saigonbank.fit.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saigonbank.fit.ims.entity.ProductGroup;

import java.util.Optional;

public interface ProductGroupRepository extends JpaRepository<ProductGroup, Long> {
    Optional<ProductGroup> findByCode(String code);
}
