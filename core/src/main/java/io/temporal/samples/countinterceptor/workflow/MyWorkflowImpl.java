package io.temporal.samples.countinterceptor.workflow;

import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

  private String name;
  private String title;
  private boolean exit = false;

  @Override
  public String exec() {

    // Wait for a greeting info
    Workflow.await(() -> name != null && title != null);

    // Execute child workflow
    ChildWorkflowOptions childWorkflowOptions =
        ChildWorkflowOptions.newBuilder().setWorkflowId("TestInterceptorChildWorkflow").build();
    MyChildWorkflow child =
        Workflow.newChildWorkflowStub(MyChildWorkflow.class, childWorkflowOptions);
    String result = child.execChild(name, title);

    // Wait for exit signal
    Workflow.await(Duration.ofSeconds(5), () -> exit != false);

    return result;
  }

  @Override
  public void signalNameAndTitle(String name, String title) {
    this.name = name;
    this.title = title;
  }

  @Override
  public String queryName() {
    return name;
  }

  @Override
  public String queryTitle() {
    return title;
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
