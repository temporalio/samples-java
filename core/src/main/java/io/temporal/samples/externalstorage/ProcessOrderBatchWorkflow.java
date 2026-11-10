package io.temporal.samples.externalstorage;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The workflow's own input and result are small. The order list passed from {@code fetchOrders} to
 * {@code summarizeOrders} is the payload large enough to be offloaded to external storage.
 */
@WorkflowInterface
public interface ProcessOrderBatchWorkflow {

  @WorkflowMethod
  BatchSummary run(int orderCount);
}
