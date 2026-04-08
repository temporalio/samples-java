package io.temporal.samples.nexus_sync_operations.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.nexus.Nexus;
import io.temporal.samples.nexus_sync_operations.service.GreetingService;
import io.temporal.samples.nexus_sync_operations.service.Language;

/**
 * Nexus operation handler implementation. Each operation is backed by the long-running
 * GreetingWorkflow entity. The operations are synchronous (sync_operation) because queries and
 * updates against a running workflow complete quickly.
 */
@ServiceImpl(service = GreetingService.class)
public class GreetingServiceImpl {

  private final String workflowId;

  public GreetingServiceImpl(String workflowId) {
    this.workflowId = workflowId;
  }

  private GreetingWorkflow getWorkflowStub() {
    return Nexus.getOperationContext()
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, workflowId);
  }

  // 👉 Backed by a query against the long-running entity workflow.
  @OperationImpl
  public OperationHandler<GreetingService.GetLanguagesInput, GreetingService.GetLanguagesOutput>
      getLanguages() {
    return OperationHandler.sync((ctx, details, input) -> getWorkflowStub().getLanguages(input));
  }

  // 👉 Backed by a query against the long-running entity workflow.
  @OperationImpl
  public OperationHandler<GreetingService.GetLanguageInput, Language> getLanguage() {
    return OperationHandler.sync((ctx, details, input) -> getWorkflowStub().getLanguage());
  }

  // 👉 Backed by an update against the long-running entity workflow. Although updates can run for
  // an arbitrarily long time, when exposed via a sync Nexus operation the update should complete
  // quickly (sync operations must finish in under 10s).
  @OperationImpl
  public OperationHandler<GreetingService.SetLanguageInput, Language> setLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> getWorkflowStub().setLanguageUsingActivity(input));
  }

  // 👉 Backed by a signal against the long-running entity workflow.
  @OperationImpl
  public OperationHandler<GreetingService.ApproveInput, GreetingService.ApproveOutput> approve() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          getWorkflowStub().approve(new GreetingWorkflow.ApproveInput(input.getName()));
          return new GreetingService.ApproveOutput();
        });
  }
}
