package io.temporal.samples.updatewithstart;

import java.util.ArrayList;
import java.util.List;

public class UpdateWithStartWorkflowState {
  private StartWorkflowRequest args;
  private List<StartWorkflowRequest> updates = new ArrayList<>();

  public UpdateWithStartWorkflowState() {}

  public UpdateWithStartWorkflowState(StartWorkflowRequest args) {
    this.args = args;
  }

  public StartWorkflowRequest getArgs() {
    return args;
  }

  public void setArgs(StartWorkflowRequest args) {
    this.args = args;
  }

  public List<StartWorkflowRequest> getUpdates() {
    return updates;
  }

  public void setUpdates(List<StartWorkflowRequest> updates) {
    this.updates = updates;
  }
}
