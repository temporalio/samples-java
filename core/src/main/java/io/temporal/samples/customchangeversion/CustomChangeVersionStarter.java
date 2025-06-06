package io.temporal.samples.customchangeversion;

import io.grpc.StatusRuntimeException;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.api.operatorservice.v1.AddSearchAttributesRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowServiceException;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.serviceclient.OperatorServiceStubs;
import io.temporal.serviceclient.OperatorServiceStubsOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.Collections;

public class CustomChangeVersionStarter {
  private static SearchAttributeKey<String> CUSTOM_CHANGE_VERSION =
      SearchAttributeKey.forKeyword("CustomChangeVersion");
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final String taskQueue = "customChangeVersionTaskQueue";
  private static final String workflowId = "CustomChangeVersionWorkflow";

  public static void main(String[] args) {
    WorkerFactory workerFactory = WorkerFactory.newInstance(client);
    Worker worker = workerFactory.newWorker(taskQueue);

    // Register CustomChangeVersion search attribute thats used in this sample
    OperatorServiceStubs operatorService =
        OperatorServiceStubs.newServiceStubs(
            OperatorServiceStubsOptions.newBuilder()
                .setChannel(service.getRawChannel())
                .validateAndBuildWithDefaults());
    operatorService
        .blockingStub()
        .addSearchAttributes(
            AddSearchAttributesRequest.newBuilder()
                .setNamespace(client.getOptions().getNamespace())
                .putAllSearchAttributes(
                    Collections.singletonMap(
                        "CustomChangeVersion", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD))
                .build());

    // Register workflow and activities
    worker.registerWorkflowImplementationTypes(CustomChangeVersionWorkflowImpl.class);
    worker.registerActivitiesImplementations(new CustomChangeVersionActivitiesImpl());
    workerFactory.start();

    CustomChangeVersionWorkflow workflow =
        client.newWorkflowStub(
            CustomChangeVersionWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(workflowId)
                .setTypedSearchAttributes(
                    SearchAttributes.newBuilder().set(CUSTOM_CHANGE_VERSION, "").build())
                .build());
    try {
      String result = workflow.run("Hello");
      System.out.println("Result: " + result);
    } catch (WorkflowServiceException e) {
      if (e.getCause() instanceof StatusRuntimeException) {
        StatusRuntimeException sre = (StatusRuntimeException) e.getCause();
        System.out.println(
            "Error starting workflow execution: "
                + sre.getMessage()
                + " Status: "
                + sre.getStatus());
      } else {
        System.out.println("Error starting workflow execution: " + e.getMessage());
      }
    }
    System.exit(0);
  }
}
