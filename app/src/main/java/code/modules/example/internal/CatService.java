package code.modules.example.internal;


import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class CatService {

  @ApplicationModuleListener
  void on(Cat event) {

  }
}