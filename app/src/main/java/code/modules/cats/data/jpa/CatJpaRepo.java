package code.modules.cats.data.jpa;

import code.modules.cats.data.entity.CatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatJpaRepo extends JpaRepository<CatEntity, Integer> {
}