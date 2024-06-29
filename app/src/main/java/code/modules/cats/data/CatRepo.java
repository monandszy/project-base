package code.modules.cats.data;

import code.modules.cats.data.dao.CatDao;
import code.modules.cats.data.jpa.CatJpaRepo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
class CatRepo implements CatDao {

  private CatJpaRepo catJpaRepo;
}