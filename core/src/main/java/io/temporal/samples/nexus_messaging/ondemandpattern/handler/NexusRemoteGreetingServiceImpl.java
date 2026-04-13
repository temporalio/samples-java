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

  static final String WORKFLOW_ID_PREFIX = "GreetingWorkflow_for_";

  // This example assumes you might have multiple workflows, one for each user.
  // If you had a single workflow for all users, then you could remove the
  // getWorkflowId method, remove the user ID from each input, and just
  // use the single worflow ID in the getWorkflowStub method below.
  public static String getWorkflowId(String userId) {
    return WORKFLOW_ID_PREFIX + userId;
  }

  private GreetingWorkflow getWorkflowStub(String userId) {
    return Nexus.getOperationContext()
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, getWorkflowId(userId));
  }

  // Starts a new GreetingWorkflow with the caller-specified workflow ID. This is an async
  // Nexus operation backed by WorkflowRunOperation.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.RunFromRemoteInput, String> runFromRemote() {
    return WorkflowRunOperation.fromWorkflowHandle(
        (ctx, details, input) -> {
          logger.info("RunFromRemote was received for userID {}", input.getUserId());
          return WorkflowHandle.fromWorkflowMethod(
              Nexus.getOperationContext()
                      .getWorkflowClient()
                      .newWorkflowStub(
                          GreetingWorkflow.class,
                          WorkflowOptions.newBuilder()
                              .setWorkflowId(getWorkflowId(input.getUserId()))
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
          logger.info("Query for GetLanguages was received for userId {}", input.getUserId());
          return getWorkflowStub(input.getUserId())
              .getLanguages(new GreetingWorkflow.GetLanguagesInput(input.isIncludeUnsupported()));
        });
  }

  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.GetLanguageInput, Language> getLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguage was received for userId {}", input.getUserId());
          return getWorkflowStub(input.getUserId()).getLanguage();
        });
  }

  // Uses setLanguageUsingActivity so that new languages are fetched via an activity.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.SetLanguageInput, Language> setLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Update for SetLanguage was received for userId {}", input.getUserId());
          return getWorkflowStub(input.getUserId())
              .setLanguageUsingActivity(new GreetingWorkflow.SetLanguageInput(input.getLanguage()));
        });
  }

  @OperationImpl
  public OperationHandler<
          NexusRemoteGreetingService.ApproveInput, NexusRemoteGreetingService.ApproveOutput>
      approve() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Signal for Approve was received for userId {}", input.getUserId());
          getWorkflowStub(input.getUserId())
              .approve(new GreetingWorkflow.ApproveInput(input.getName()));
          return new NexusRemoteGreetingService.ApproveOutput();
        });
  }
}
