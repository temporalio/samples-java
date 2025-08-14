package io.temporal.samples.nexusexternalcaller.caller;

import io.nexusrpc.OperationException;
import io.nexusrpc.OperationStillRunningException;
import io.nexusrpc.client.FetchOperationResultOptions;
import io.nexusrpc.client.OperationHandle;
import io.nexusrpc.client.ServiceClient;
import io.nexusrpc.client.StartOperationResponse;
import io.temporal.client.TemporalNexusServiceClientOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexus.service.NexusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class CallerStarter {
  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  public static void main(String[] args) throws OperationStillRunningException, OperationException {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    ServiceClient<NexusService> serviceClient =
        client.newNexusServiceClient(
            NexusService.class,
            TemporalNexusServiceClientOptions.newBuilder()
                .setEndpoint("my-nexus-endpoint-name")
                .build());

    // Execute a synchronous operation
    NexusService.EchoOutput result =
        serviceClient.executeOperation(NexusService::echo, new NexusService.EchoInput("Hello"));
    logger.info("Execute echo operation: {}", result.getMessage());
    // Start an asynchronous operation
    StartOperationResponse<NexusService.HelloOutput> response =
        serviceClient.startOperation(
            NexusService::hello, new NexusService.HelloInput("Hello", NexusService.Language.EN));
    if (!(response instanceof StartOperationResponse.Async)) {
      throw new IllegalStateException("Expected an asynchronous operation response");
    }
    OperationHandle<NexusService.HelloOutput> handle =
        ((StartOperationResponse.Async<NexusService.HelloOutput>) response).getHandle();
    logger.info("Started hello operation with token: {}", handle.getOperationToken());
    // Wait for the operation to complete
    logger.info("Waiting for hello operation to complete...");
    NexusService.HelloOutput helloResult =
        handle.fetchResult(
            FetchOperationResultOptions.newBuilder().setTimeout(Duration.ofSeconds(5)).build());
    logger.info("Hello operation result: {}", helloResult.getMessage());
    // We can also get the status of an operation
    logger.info("Operation state: {}", handle.getInfo().getState());
  }
}
