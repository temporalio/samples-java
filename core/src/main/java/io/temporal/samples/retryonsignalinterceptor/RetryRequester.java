package io.temporal.samples.retryonsignalinterceptor;

import static io.temporal.samples.retryonsignalinterceptor.MyWorkflowWorker.WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

public class RetryRequester {

  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    // Note that we use the listener interface that the interceptor registered dynamically, not the
    // workflow interface.
    RetryOnSignalInterceptorListener workflow =
        client.newWorkflowStub(RetryOnSignalInterceptorListener.class, WORKFLOW_ID);

    // Sends "Retry" signal to the workflow.
    workflow.retry();

    System.out.println("\"Retry\" signal sent");
    System.exit(0);
  }
}
