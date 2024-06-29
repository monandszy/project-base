package code.modules.cats.web;

import code.modules.cats.service.CatiumCommand;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
class CatModifier {
  private CatiumCommand catiumCommand;
}