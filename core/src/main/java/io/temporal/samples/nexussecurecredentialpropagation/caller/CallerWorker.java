package io.temporal.samples.nexussecurecredentialpropagation.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleConfig;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;

public class CallerWorker {
  public static void main(String[] args) {
    // The caller Worker holds the Workflow credential private key (it receives the start hop) and
    // only the public key of the Nexus handler (it sends the Nexus hop).
    SampleKeysets keys = SampleKeysets.fromDefaultDirectory();
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder()
                .setDataConverter(SampleConfig.dataConverter(keys.payloadAead())));

    WorkerFactory factory =
        WorkerFactory.newInstance(
            client,
            WorkerFactoryOptions.newBuilder()
                // Must be registered last; see CredentialCallerWorkerInterceptor.
                .setWorkerInterceptors(
                    new CredentialCallerWorkerInterceptor(
                        keys.workflowCredentialDecrypt(),
                        keys.workflowCredentialEncrypt(),
                        keys.nexusCredentialEncrypt(),
                        SampleConfig.PROTECTED_ROUTES))
                .build());

    Worker worker = factory.newWorker(SampleConfig.CALLER_TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                Collections.singletonMap(
                    TransferService.NAME,
                    NexusServiceOptions.newBuilder().setEndpoint(SampleConfig.ENDPOINT).build()))
            .build(),
        TransferWorkflowImpl.class);

    factory.start();
  }
}
