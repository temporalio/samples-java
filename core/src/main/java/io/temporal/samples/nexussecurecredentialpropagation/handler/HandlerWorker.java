package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexussecurecredentialpropagation.common.DemoIdentityProvider;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleConfig;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import java.util.Collections;

public class HandlerWorker {
  public static void main(String[] args) {
    // The handler is the only holder of the Nexus credential private key and the operation token
    // key.
    SampleKeysets keys = SampleKeysets.fromDefaultDirectory();
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder()
                .setDataConverter(SampleConfig.dataConverter(keys.payloadAead())));

    CredentialHandlerWorkerInterceptor interceptor =
        new CredentialHandlerWorkerInterceptor(
            keys.nexusCredentialDecrypt(),
            new JwtVerifier(
                keys.demoIdentityProviderVerifier(),
                DemoIdentityProvider.ISSUER,
                DemoIdentityProvider.HANDLER_AUDIENCE),
            Collections.singletonMap(
                TransferService.NAME + "/" + TransferService.TRANSFER, new TransferIntentPolicy()));

    WorkerFactory factory =
        WorkerFactory.newInstance(
            client, WorkerFactoryOptions.newBuilder().setWorkerInterceptors(interceptor).build());
    Worker worker = factory.newWorker(SampleConfig.HANDLER_TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(TransferHandlerWorkflowImpl.class);
    worker.registerNexusServiceImplementation(
        new TransferNexusServiceImpl(keys.operationTokenMac()));
    factory.start();
  }
}
