package io.temporal.samples.batch.iterator;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/** Workflow that implements processing of a single record. */
@WorkflowInterface
public interface SingleListingMigrationWorkflow {

  /** Processes a single record */
  @WorkflowMethod
  SingleResponse processRecord(SingleRecord r);
}
