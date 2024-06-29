package code.modules.cats.service;

import code.modules.cats.data.dao.CatDao;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CatiumCommand {
  private CatDao catDao;
}