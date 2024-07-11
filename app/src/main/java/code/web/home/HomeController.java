package code.web.home;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
class HomeController {

  @GetMapping(value = "/")
  String getHome() {
    return "index";
  }

  @GetMapping(value = "/catnip")
  String getCatnip() {
    return "catnip";
  }


}