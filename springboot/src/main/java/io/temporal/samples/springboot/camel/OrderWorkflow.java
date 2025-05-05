

package io.temporal.samples.springboot.camel;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface OrderWorkflow {
  @WorkflowMethod
  public List<OfficeOrder> start();
}
