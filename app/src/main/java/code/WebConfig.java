package code;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class WebConfig {

  @GetMapping(value = "/")
  public String getHome() {
    return "index";
  }

}