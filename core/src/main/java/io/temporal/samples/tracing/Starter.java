package io.temporal.samples.tracing;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.samples.tracing.workflow.TracingWorkflow;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Starter {
  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  public static final String TASK_QUEUE_NAME = "tracingTaskQueue";

  public static void main(String[] args) {
    String type = "OpenTelemetry";
    if (args.length == 1) {
      type = args[0];
    }

    // Set the OpenTracing client interceptor
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder()
            .setInterceptors(new OpenTracingClientInterceptor(JaegerUtils.getJaegerOptions(type)))
            .build();
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("tracingWorkflow")
            .setTaskQueue(TASK_QUEUE_NAME)
            .build();

    // Create typed workflow stub
    TracingWorkflow workflow = client.newWorkflowStub(TracingWorkflow.class, workflowOptions);

    // Convert to untyped and start it with signalWithStart
    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    untyped.signalWithStart("setLanguage", new Object[] {"Spanish"}, new Object[] {"John"});

    String greeting = untyped.getResult(String.class);

    System.out.println("Greeting: " + greeting);

    System.exit(0);
  }
}
