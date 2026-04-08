package io.temporal.samples.nexus_messaging.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.nexus.Nexus;
import io.temporal.samples.nexus_messaging.service.Language;
import io.temporal.samples.nexus_messaging.service.NexusGreetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus operation handler implementation. Each operation is backed by the long-running
 * GreetingWorkflow entity. The operations are synchronous (sync_operation) because queries and
 * updates against a running workflow complete quickly.
 */
@ServiceImpl(service = NexusGreetingService.class)
public class NexusGreetingServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(NexusGreetingServiceImpl.class);

  private final String workflowId;

  public NexusGreetingServiceImpl(String workflowId) {
    this.workflowId = workflowId;
  }

  private GreetingWorkflow getWorkflowStub() {
    return Nexus.getOperationContext()
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, workflowId);
  }

  // 👉 Backed by a query against the long-running entity workflow.
  @OperationImpl
  public OperationHandler<
          NexusGreetingService.GetLanguagesInput, NexusGreetingService.GetLanguagesOutput>
      getLanguages() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguages was received");
          return getWorkflowStub().getLanguages(input);
        });
  }

  // 👉 Backed by a query against the long-running entity workflow.
  @OperationImpl
  public OperationHandler<NexusGreetingService.GetLanguageInput, Language> getLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguage was received");
          return getWorkflowStub().getLanguage();
        });
  }

  // 👉 Backed by an update against the long-running entity workflow. Although updates can run for
  // an arbitrarily long time, when exposed via a sync Nexus operation the update should complete
  // quickly (sync operations must finish in under 10s).
  @OperationImpl
  public OperationHandler<NexusGreetingService.SetLanguageInput, Language> setLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Update for SetLanguage was received");
          return getWorkflowStub().setLanguageUsingActivity(input);
        });
  }

  // 👉 Backed by a signal against the long-running entity workflow.
  @OperationImpl
  public OperationHandler<NexusGreetingService.ApproveInput, NexusGreetingService.ApproveOutput>
      approve() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Signal for Approve was received");
          getWorkflowStub().approve(new GreetingWorkflow.ApproveInput(input.getName()));
          return new NexusGreetingService.ApproveOutput();
        });
  }
}
