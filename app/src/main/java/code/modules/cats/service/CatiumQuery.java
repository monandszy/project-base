package code.modules.cats.service;

import code.modules.cats.CatFacade;
import code.modules.cats.data.dao.CatDao;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CatiumQuery implements CatFacade {
  private CatDao catDao;
}