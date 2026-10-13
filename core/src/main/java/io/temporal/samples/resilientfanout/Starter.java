package io.temporal.samples.resilientfanout;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class Starter {

  public static final String TASK_QUEUE = "resilientFanoutTaskQueue";

  public static void main(String[] args) {
    // args[0]: "user-critical-failure", "user-degraded", or anything else (happy path).
    String userId = args.length > 0 ? args[0] : "user-1";

    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        OnboardingWorkflowImpl.class,
        CreateAccountWorkflowImpl.class,
        ProvisionStorageWorkflowImpl.class,
        SendWelcomeEmailWorkflowImpl.class);
    worker.registerActivitiesImplementations(
        new OnboardingActivitiesImpl(), new ReconciliationActivitiesImpl());
    factory.start();

    OnboardingWorkflow workflow =
        client.newWorkflowStub(
            OnboardingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("onboarding-" + userId)
                .setTaskQueue(TASK_QUEUE)
                .build());

    try {
      OnboardingResult result = workflow.onboard(userId);
      System.out.println("Onboarding succeeded: " + result);
    } catch (Exception e) {
      System.out.println("Onboarding failed: " + e.getMessage());
    }

    System.exit(0);
  }
}
