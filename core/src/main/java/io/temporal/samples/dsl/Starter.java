package io.temporal.samples.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.samples.dsl.model.Flow;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class Starter {

  public static void main(String[] args) {
    Flow flow = getFlowFromResource();

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
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("dsl-task-queue");
    worker.registerWorkflowImplementationTypes(DslWorkflowImpl.class);
    worker.registerActivitiesImplementations(new DslActivitiesImpl());
    factory.start();

    DslWorkflow workflow =
        client.newWorkflowStub(
            DslWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("dsl-workflow")
                .setTaskQueue("dsl-task-queue")
                .build());

    String result = workflow.run(flow, "sample input");

    System.out.println("Result: " + result);

    System.exit(0);
  }

  private static Flow getFlowFromResource() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(
          Starter.class.getClassLoader().getResource("dsl/sampleflow.json"), Flow.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
