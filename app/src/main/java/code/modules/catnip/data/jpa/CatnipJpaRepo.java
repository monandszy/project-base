package code.modules.catnip.data.jpa;

import code.modules.catnip.data.entity.CatnipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatnipJpaRepo extends JpaRepository<CatnipEntity, Integer> {
}