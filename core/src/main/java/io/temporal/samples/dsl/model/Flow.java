package io.temporal.samples.dsl.model;

import java.util.List;

public class Flow {
  private String id;
  private String name;
  private String description;
  private List<FlowAction> actions;

  public Flow() {}

  public Flow(String id, String name, String description, List<FlowAction> actions) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.actions = actions;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<FlowAction> getActions() {
    return actions;
  }

  public void setActions(List<FlowAction> actions) {
    this.actions = actions;
  }
}
