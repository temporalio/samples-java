package io.temporal.samples.helloworld;
// @@@SNIPSTART java-hello-world-sample-workflow-interface
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorldWorkflow {

  @WorkflowMethod
  String getGreeting(String name);
}
// @@@SNIPEND
