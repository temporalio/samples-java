package io.temporal.samples.complex;

import static io.temporal.samples.complex.ComplexWorkflow.TASK_QUEUE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.ByteArrayPayloadConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.common.converter.NullPayloadConverter;
import io.temporal.common.converter.ProtobufJsonPayloadConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Starter {
  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    DefaultDataConverter dataConverter =
        new DefaultDataConverter(
            new NullPayloadConverter(),
            new ByteArrayPayloadConverter(),
            new ProtobufJsonPayloadConverter(),
            new JacksonJsonPayloadConverter(mapper));

    WorkflowClientOptions options =
        WorkflowClientOptions.newBuilder().setDataConverter(dataConverter).build();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service, options);

    for (int i = 0; i < 1000; i++) {
      ComplexWorkflow workflow =
          client.newWorkflowStub(
              ComplexWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
      // Execute a workflow waiting for it to complete. See {@link
      // io.temporal.samples.hello.HelloSignal}
      // for an example of starting workflow without waiting synchronously for its result.

      WorkflowClient.start(workflow::handleLambda, new Input(i));
      System.out.println("done " + i);
    }
    System.exit(0);
  }
}
