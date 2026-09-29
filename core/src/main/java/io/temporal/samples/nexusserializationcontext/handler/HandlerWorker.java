package io.temporal.samples.nexusserializationcontext.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HandlerWorker {
  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder().setDataConverter(SampleConfig.dataConverter()));
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(SampleConfig.HANDLER_TASK_QUEUE);
    worker.registerNexusServiceImplementation(new EchoServiceImpl());

    factory.start();
  }
}
