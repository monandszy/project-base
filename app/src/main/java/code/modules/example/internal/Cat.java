package code.modules.example.internal;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
class Cat {
  String name;
}