

package io.temporal.samples.springboot.hello;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.springboot.hello.model.Person;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(taskQueues = "HelloSampleTaskQueue")
public class HelloWorkflowImpl implements HelloWorkflow {

  private HelloActivity activity =
      Workflow.newActivityStub(
          HelloActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public String sayHello(Person person) {
    return activity.hello(person);
  }
}
