package code.modules.cats.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CatEntity {
  @Id
  private Long id;

}