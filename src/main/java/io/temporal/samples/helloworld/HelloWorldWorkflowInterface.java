package io.temporal.samples.helloworld;
// @@@START java-hello-world-sample-workflow-interface
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorldWorkflowInterface {

  @WorkflowMethod
  String getGreeting(String name);
}
// @@@END