package code;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.modulith.Modulithic;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.TimeZone;


@Modulithic
@SpringBootApplication
//@ComponentScans({
//    @ComponentScan(basePackages = "code", includeFilters = {@Filter(Component.class)}),
//    @ComponentScan(basePackages = "code.web", includeFilters = {@Filter(Controller.class)}),
//    @ComponentScan(basePackages = "code.modules", includeFilters = {@Filter(Service.class), @Filter(Repository.class),}),
//})
public class App extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(App.class, "--debug");
  }

  @PostConstruct
  public void setTimeZone(){
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @Bean
  Clock customClock() {
    return Clock.system(ZoneOffset.UTC);
  }
}