package io.temporal.samples.nexussecurecredentialpropagation.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.DemoIdentityProvider;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleConfig;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerStarter {
  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  public static void main(String[] args) {
    // The starter holds only the caller Worker's public key: it can seal credentials for the
    // Workflow but cannot read any captured from history.
    SampleKeysets keys = SampleKeysets.fromDefaultDirectory();
    WorkflowClient base =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder()
                .setDataConverter(SampleConfig.dataConverter(keys.payloadAead())));
    WorkflowClient client =
        WorkflowClient.newInstance(
            base.getWorkflowServiceStubs(),
            WorkflowClientOptions.newBuilder(base.getOptions())
                // Must be registered first; see CredentialWorkflowClientInterceptor.
                .setInterceptors(
                    new CredentialWorkflowClientInterceptor(
                        base.getOptions().getNamespace(), keys.workflowCredentialEncrypt()))
                .build());

    TransferService.TransferInput input =
        new TransferService.TransferInput("acme", "alice", "payment-1001", "bob", 25_00);

    // Stand-in for a token exchange at the client edge: a short-lived token restricted to the
    // handler's audience.
    String jwt =
        new DemoIdentityProvider(keys.demoIdentityProviderSigner())
            .issue("alice", "acme", TransferService.REQUIRED_SCOPE, Duration.ofMinutes(5));

    TransferWorkflow workflow =
        client.newWorkflowStub(
            TransferWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("transfer-" + input.getPaymentId())
                .setTaskQueue(SampleConfig.CALLER_TASK_QUEUE)
                .build());
    CredentialContext.callWith(
        new CredentialEnvelope(jwt), () -> WorkflowClient.start(workflow::transfer, input));
    TransferService.TransferOutput output = workflow.transfer(input);
    logger.info(
        "Transfer {} {} for {}",
        output.getPaymentId(),
        output.getStatus(),
        output.getApprovedFor());
    System.exit(0);
  }
}
