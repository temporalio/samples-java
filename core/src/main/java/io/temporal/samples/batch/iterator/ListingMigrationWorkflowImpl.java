package io.temporal.samples.batch.iterator;

import io.temporal.workflow.Workflow;

public class ListingMigrationWorkflowImpl implements ListingMigrationWorkflow {
  @Override
  public void execute() {
    var iteratorBatchWorkflow = Workflow.newChildWorkflowStub(IteratorBatchWorkflow.class);
    iteratorBatchWorkflow.processBatch(100, 0);
  }
}
