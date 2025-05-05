package io.temporal.samples.retryonsignalinterceptor;

import static io.temporal.samples.retryonsignalinterceptor.MyWorkflowWorker.WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class QueryRequester {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Note that we use the listener interface that the interceptor registered dynamically, not the
    // workflow interface.
    RetryOnSignalInterceptorListener workflow =
        client.newWorkflowStub(RetryOnSignalInterceptorListener.class, WORKFLOW_ID);

    // Queries workflow.
    String status = workflow.getPendingActivitiesStatus();

    System.out.println("Workflow Pending Activities Status:\n\n" + status);
    System.exit(0);
  }
}
