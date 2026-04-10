package io.temporal.samples.nexus_messaging.ondemandpattern.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowHandle;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexus_messaging.ondemandpattern.service.Language;
import io.temporal.samples.nexus_messaging.ondemandpattern.service.NexusRemoteGreetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus operation handler for the on-demand pattern. Each operation receives the target workflow ID
 * in its input, and {@code runFromRemote} starts a brand-new GreetingWorkflow.
 */
@ServiceImpl(service = NexusRemoteGreetingService.class)
public class NexusRemoteGreetingServiceImpl {

  private static final Logger logger =
      LoggerFactory.getLogger(NexusRemoteGreetingServiceImpl.class);

  private GreetingWorkflow getWorkflowStub(String workflowId) {
    return Nexus.getOperationContext()
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, workflowId);
  }

  // Starts a new GreetingWorkflow with the caller-specified workflow ID. This is an async
  // Nexus operation backed by WorkflowRunOperation.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.RunFromRemoteInput, String> runFromRemote() {
    return WorkflowRunOperation.fromWorkflowHandle(
        (ctx, details, input) -> {
          logger.info("RunFromRemote was received for workflow {}", input.getWorkflowId());
          return WorkflowHandle.fromWorkflowMethod(
              Nexus.getOperationContext()
                      .getWorkflowClient()
                      .newWorkflowStub(
                          GreetingWorkflow.class,
                          WorkflowOptions.newBuilder()
                              .setWorkflowId(input.getWorkflowId())
                              .setTaskQueue(HandlerWorker.TASK_QUEUE)
                              .build())
                  ::run);
        });
  }

  @OperationImpl
  public OperationHandler<
          NexusRemoteGreetingService.GetLanguagesInput,
          NexusRemoteGreetingService.GetLanguagesOutput>
      getLanguages() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguages was received for workflow {}", input.getWorkflowId());
          return getWorkflowStub(input.getWorkflowId())
              .getLanguages(new GreetingWorkflow.GetLanguagesInput(input.isIncludeUnsupported()));
        });
  }

  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.GetLanguageInput, Language> getLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguage was received for workflow {}", input.getWorkflowId());
          return getWorkflowStub(input.getWorkflowId()).getLanguage();
        });
  }

  // Uses setLanguageUsingActivity so that new languages are fetched via an activity.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.SetLanguageInput, Language> setLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Update for SetLanguage was received for workflow {}", input.getWorkflowId());
          return getWorkflowStub(input.getWorkflowId())
              .setLanguageUsingActivity(new GreetingWorkflow.SetLanguageInput(input.getLanguage()));
        });
  }

  @OperationImpl
  public OperationHandler<
          NexusRemoteGreetingService.ApproveInput, NexusRemoteGreetingService.ApproveOutput>
      approve() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Signal for Approve was received for workflow {}", input.getWorkflowId());
          getWorkflowStub(input.getWorkflowId())
              .approve(new GreetingWorkflow.ApproveInput(input.getName()));
          return new NexusRemoteGreetingService.ApproveOutput();
        });
  }
}
