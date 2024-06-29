package code.modules.cats.web;

import code.modules.cats.service.CatiumQuery;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
class CatRetriever {
  private CatiumQuery catiumQuery;
}