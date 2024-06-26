package code;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.TimeZone;

@SpringBootApplication
public class App extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @PostConstruct
  public void init(){
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}