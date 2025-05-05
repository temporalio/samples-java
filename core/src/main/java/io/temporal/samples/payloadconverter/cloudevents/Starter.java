package io.temporal.samples.payloadconverter.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Starter {

  private static final String TASK_QUEUE = "CloudEventsConverterQueue";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    // Add CloudEventsPayloadConverter
    // It has the same encoding type as JacksonJsonPayloadConverter
    DefaultDataConverter ddc =
        DefaultDataConverter.newDefaultInstance()
            .withPayloadConverterOverrides(new CloudEventsPayloadConverter());

    WorkflowClientOptions workflowClientOptions =
        WorkflowClientOptions.newBuilder().setDataConverter(ddc).build();

    WorkflowClient client = WorkflowClient.newInstance(service, workflowClientOptions);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(CEWorkflowImpl.class);

    factory.start();

    WorkflowOptions newCustomerWorkflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();

    CEWorkflow workflow = client.newWorkflowStub(CEWorkflow.class, newCustomerWorkflowOptions);

    // Create 10 cloud events
    List<CloudEvent> cloudEventList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      cloudEventList.add(
          CloudEventBuilder.v1()
              .withId(String.valueOf(100 + i))
              .withType("example.demo")
              .withSource(URI.create("http://temporal.io"))
              .withData(
                  "application/json",
                  ("{\n" + "\"greeting\": \"hello " + i + "\"\n" + "}")
                      .getBytes(Charset.defaultCharset()))
              .build());
    }

    WorkflowClient.start(workflow::exec, cloudEventList.get(0));

    // Send signals (cloud event data)
    for (int j = 1; j < 10; j++) {
      workflow.addEvent(cloudEventList.get(j));
    }

    // Get the CE result and get its data (JSON)
    String result =
        ((JsonCloudEventData) workflow.getLastEvent().getData()).getNode().get("greeting").asText();

    System.out.println("Last event body: " + result);

    System.exit(0);
  }
}
