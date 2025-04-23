package io.temporal.samples.updatewithstart;

import java.util.ArrayList;
import java.util.List;

public class UpdateWithStartWorkflowState {
  private StartWorkflowRequest initArgs;
  private StartWorkflowRequest executeArgs;
  private List<StartWorkflowRequest> updates = new ArrayList<>();

  public UpdateWithStartWorkflowState() {}

  public StartWorkflowRequest getInitArgs() {
    return initArgs;
  }

  public void setInitArgs(StartWorkflowRequest initArgs) {
    this.initArgs = initArgs;
  }

  public List<StartWorkflowRequest> getUpdates() {
    return updates;
  }

  public void setUpdates(List<StartWorkflowRequest> updates) {
    this.updates = updates;
  }

  public StartWorkflowRequest getExecuteArgs() {
    return executeArgs;
  }

  public void setExecuteArgs(StartWorkflowRequest executeArgs) {
    this.executeArgs = executeArgs;
  }
}
