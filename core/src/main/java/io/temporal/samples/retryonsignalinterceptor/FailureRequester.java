

package io.temporal.samples.retryonsignalinterceptor;

import static io.temporal.samples.retryonsignalinterceptor.MyWorkflowWorker.WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;

/**
 * Send signal requesting that an exception thrown from the activity is propagated to the workflow.
 */
public class FailureRequester {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Note that we use the listener interface that the interceptor registered dynamically, not the
    // workflow interface.
    RetryOnSignalInterceptorListener workflow =
        client.newWorkflowStub(RetryOnSignalInterceptorListener.class, WORKFLOW_ID);

    // Sends "Fail" signal to the workflow.
    workflow.fail();

    System.out.println("\"Fail\" signal sent");
    System.exit(0);
  }
}
