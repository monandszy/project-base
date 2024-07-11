package code.modules.catnip.data;

import code.modules.catnip.data.dao.CatDao;
import code.modules.catnip.data.jpa.CatnipJpaRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
class CatnipRepo implements CatDao {

  private CatnipJpaRepo catnipJpaRepo;
}