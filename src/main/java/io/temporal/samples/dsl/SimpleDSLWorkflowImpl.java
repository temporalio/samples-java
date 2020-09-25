package io.temporal.samples.dsl;

import io.temporal.samples.dsl.models.Workflow;
import java.util.Map;

public class SimpleDSLWorkflowImpl implements SimpleDSLWorkflow {
  private Map<String, String> bindings;

  @Override
  public void execute(Workflow dsl) {
    this.bindings = dsl.variables;
    dsl.root.execute(this.bindings);
  }
}
