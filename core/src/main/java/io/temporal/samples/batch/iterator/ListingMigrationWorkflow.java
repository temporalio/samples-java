package io.temporal.samples.batch.iterator;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ListingMigrationWorkflow {

  @WorkflowMethod
  void execute();
}
