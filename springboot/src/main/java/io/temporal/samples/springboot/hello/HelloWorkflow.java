

package io.temporal.samples.springboot.hello;

import io.temporal.samples.springboot.hello.model.Person;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorkflow {
  @WorkflowMethod
  String sayHello(Person person);
}
