package io.temporal.samples.nexusmessaging.callerpattern.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.nexus.TemporalNexusClient;
import io.temporal.nexus.TemporalOperationHandler;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.samples.nexusmessaging.callerpattern.service.Language;
import io.temporal.samples.nexusmessaging.callerpattern.service.NexusGreetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus operation handler implementation. Each operation receives a userId, which is mapped to a
 * workflow ID using {@link #WORKFLOW_ID_PREFIX}.
 *
 * <p>Every operation is a {@link TemporalOperationHandler}: the start handler receives a {@link
 * TemporalNexusClient} scoped to the invocation and returns a {@link TemporalOperationResult}. The
 * queries and the signal complete inline and return a sync result; {@code setLanguage} starts the
 * update through the Nexus client, which makes it an asynchronous operation.
 */
@ServiceImpl(service = NexusGreetingService.class)
public class NexusGreetingServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(NexusGreetingServiceImpl.class);

  static final String WORKFLOW_ID_PREFIX = "GreetingWorkflow_for_";

  // This example assumes you might have multiple workflows, one for each user.
  // If you had a single workflow for all users, then you could remove the
  // getWorkflowId method, remove the user ID from each input, and just
  // use the single workflow ID in the getWorkflowStub method below.
  public static String getWorkflowId(String userId) {
    return WORKFLOW_ID_PREFIX + userId;
  }

  private GreetingWorkflow getWorkflowStub(TemporalNexusClient client, String userId) {
    return client
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, getWorkflowId(userId));
  }

  @OperationImpl
  public OperationHandler<
          NexusGreetingService.GetLanguagesInput, NexusGreetingService.GetLanguagesOutput>
      getLanguages() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Query for GetLanguages was received for user {}", input.getUserId());
          return TemporalOperationResult.sync(
              getWorkflowStub(client, input.getUserId()).getLanguages(input));
        });
  }

  @OperationImpl
  public OperationHandler<NexusGreetingService.GetLanguageInput, Language> getLanguage() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Query for GetLanguage was received for user {}", input.getUserId());
          return TemporalOperationResult.sync(
              getWorkflowStub(client, input.getUserId()).getLanguage());
        });
  }

  // Routes to setLanguageUsingActivity (not setLanguage) so that new languages not already in the
  // greetings map can be fetched via an activity.
  //
  // Starting the update through the Nexus client makes this an asynchronous Nexus operation: the
  // caller receives an operation token and the update result is delivered later over the Nexus
  // completion callback. If the update is already complete when the start call returns, the result
  // comes back synchronously instead.
  @OperationImpl
  public OperationHandler<NexusGreetingService.SetLanguageInput, Language> setLanguage() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Update for SetLanguage was received for user {}", input.getUserId());
          return client.startWorkflowUpdate(
              GreetingWorkflow.class,
              getWorkflowId(input.getUserId()),
              GreetingWorkflow::setLanguageUsingActivity,
              input,
              UpdateOptions.newBuilder(Language.class)
                  .setUpdateName("setLanguageUsingActivity")
                  .setWaitForStage(WorkflowUpdateStage.ACCEPTED)
                  .build());
        });
  }

  @OperationImpl
  public OperationHandler<NexusGreetingService.ApproveInput, NexusGreetingService.ApproveOutput>
      approve() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Signal for Approve was received for user {}", input.getUserId());
          getWorkflowStub(client, input.getUserId()).approve(input);
          return TemporalOperationResult.sync(new NexusGreetingService.ApproveOutput());
        });
  }
}
