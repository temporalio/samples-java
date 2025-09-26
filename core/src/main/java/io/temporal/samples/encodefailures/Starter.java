package io.temporal.samples.encodefailures;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import java.io.IOException;
import java.util.Collections;

public class Starter {
  private static final String TASK_QUEUE = "EncodeDecodeFailuresTaskQueue";
  private static final String WORKFLOW_ID = "CustomerValidationWorkflow";

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

    // CodecDataConverter defines our data converter and codec
    // sets encodeFailureAttributes to true
    CodecDataConverter codecDataConverter =
        new CodecDataConverter(
            // For sample we just use default data converter
            DefaultDataConverter.newDefaultInstance(),
            // Simple prefix codec to encode/decode
            Collections.singletonList(new SimplePrefixPayloadCodec()),
            true); // Setting encodeFailureAttributes to true

    // WorkflowClient uses our CodecDataConverter
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setDataConverter(codecDataConverter).build());

    // Create worker and start Worker factory
    createWorker(client);

    // Start workflow execution and catch client error (workflow execution fails)
    CustomerAgeCheck workflow =
        client.newWorkflowStub(
            CustomerAgeCheck.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    try {
      // Start workflow execution to validate under-age customer
      workflow.validateCustomer(new MyCustomer("John", 17));
      System.out.println("Workflow should have failed on customer validation");
    } catch (WorkflowFailedException e) {
      // Get failure message from last event in history (WorkflowExecutionFailed event) and check
      // that
      // its encoded
      HistoryEvent wfExecFailedEvent = client.fetchHistory(WORKFLOW_ID).getLastEvent();
      Payload payload =
          wfExecFailedEvent
              .getWorkflowExecutionFailedEventAttributes()
              .getFailure()
              .getEncodedAttributes();
      if (isEncoded(payload)) {
        System.out.println("Workflow failure was encoded");
      } else {
        System.out.println("Workflow failure was not encoded");
      }
    }

    // Stop sample
    System.exit(0);
  }

  private static boolean isEncoded(Payload payload) {
    return payload.getData().startsWith(SimplePrefixPayloadCodec.PREFIX);
  }

  private static void createWorker(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            // note we set InvalidCustomerException to fail execution
            .setFailWorkflowExceptionTypes(InvalidCustomerException.class)
            .build(),
        CustomerAgeCheckImpl.class);
    factory.start();
  }
}
