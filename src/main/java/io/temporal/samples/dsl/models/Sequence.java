package io.temporal.samples.dsl.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class Sequence {
  public Statement elements[];

  @JsonCreator
  public Sequence(@JsonProperty("elements") Statement elements[]) {
    this.elements = elements;
  }

  public void execute(Map<String, String> bindings) {
    for (Statement s : elements) {
      s.execute(bindings);
    }
  }
}
