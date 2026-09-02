package io.temporal.samples.nexusmessaging.ondemandpattern.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.BatchRequest;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.nexus.TemporalNexusClient;
import io.temporal.nexus.TemporalOperationHandler;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.samples.nexusmessaging.ondemandpattern.service.Language;
import io.temporal.samples.nexusmessaging.ondemandpattern.service.NexusRemoteGreetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus Operation handler for the on-demand pattern. Each Operation receives a userId, which {@link
 * #getWorkflowId} maps to the target Workflow ID, and {@code runFromRemote} starts the
 * GreetingWorkflow for that user.
 */
@ServiceImpl(service = NexusRemoteGreetingService.class)
public class NexusRemoteGreetingServiceImpl {

  private static final Logger logger =
      LoggerFactory.getLogger(NexusRemoteGreetingServiceImpl.class);

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

  // Starts the GreetingWorkflow for the given user, or attaches to one already running (see the
  // conflict policy below). startWorkflow attaches a completion callback, so the Operation
  // completes when the Workflow returns.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.RunFromRemoteInput, String> runFromRemote() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("RunFromRemote was received for userID {}", input.getUserId());
          return client.startWorkflow(
              GreetingWorkflow.class,
              GreetingWorkflow::run,
              WorkflowOptions.newBuilder()
                  .setWorkflowId(getWorkflowId(input.getUserId()))
                  .setTaskQueue(HandlerWorker.TASK_QUEUE)
                  // By default, starting a Workflow whose ID is already running fails the
                  // Operation. Since attachApprovalContext below can create the GreetingWorkflow
                  // first, this Operation needs to attach to the running execution rather than
                  // fail.
                  .setWorkflowIdConflictPolicy(
                      WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                  .build());
        });
  }

  @OperationImpl
  public OperationHandler<
          NexusRemoteGreetingService.GetLanguagesInput,
          NexusRemoteGreetingService.GetLanguagesOutput>
      getLanguages() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Query for GetLanguages was received for userId {}", input.getUserId());
          return TemporalOperationResult.sync(
              getWorkflowStub(client, input.getUserId())
                  .getLanguages(
                      new GreetingWorkflow.GetLanguagesInput(input.isIncludeUnsupported())));
        });
  }

  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.GetLanguageInput, Language> getLanguage() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Query for GetLanguage was received for userId {}", input.getUserId());
          return TemporalOperationResult.sync(
              getWorkflowStub(client, input.getUserId()).getLanguage());
        });
  }

  // Uses setLanguageUsingActivity so that new languages are fetched via an activity.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.SetLanguageInput, Language> setLanguage() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Update for SetLanguage was received for userId {}", input.getUserId());
          return client.startWorkflowUpdate(
              GreetingWorkflow.class,
              getWorkflowId(input.getUserId()),
              GreetingWorkflow::setLanguageUsingActivity,
              new GreetingWorkflow.SetLanguageInput(input.getLanguage()),
              UpdateOptions.<Language>newBuilder()
                  .setResultClass(Language.class)
                  // The Update to invoke has to be named explicitly; the method reference above
                  // supplies the argument and result types but not the wire name.
                  .setUpdateName(GreetingWorkflow.SET_LANGUAGE_USING_ACTIVITY_UPDATE)
                  // An Update-backed Operation must wait for the ACCEPTED stage. Any other stage
                  // is rejected with "nexus op workflow updates only support
                  // WorkflowUpdateStageAccepted for async updates".
                  .setWaitForStage(WorkflowUpdateStage.ACCEPTED)
                  .build());
        });
  }

  @OperationImpl
  public OperationHandler<
          NexusRemoteGreetingService.ApproveInput, NexusRemoteGreetingService.ApproveOutput>
      approve() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info("Signal for Approve was received for userId {}", input.getUserId());
          getWorkflowStub(client, input.getUserId())
              .approve(new GreetingWorkflow.ApproveInput(input.getName()));
          return TemporalOperationResult.sync(new NexusRemoteGreetingService.ApproveOutput());
        });
  }

  // Signal-with-Start. Supporting information for an approval is often produced by a different
  // system than the one requesting it, so the two messages can arrive in either order. This
  // Operation is written so that either works, which means it may have to create the Workflow
  // itself: signalWithStart delivers the Signal, starting the Workflow first if it is not already
  // running. When the Workflow already exists, only the Signal is delivered.
  //
  // Both this and runFromRemote derive the same Workflow ID from the same userId, which is what
  // lets them agree on which execution they mean regardless of which arrives first.
  //
  // Like approve, this is sync messaging: it completes during the handler call.
  @OperationImpl
  public OperationHandler<NexusRemoteGreetingService.AttachApprovalContextInput, Void>
      attachApprovalContext() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          logger.info(
              "AttachApprovalContext was received for userId {}: {}",
              input.getUserId(),
              input.getNote());
          WorkflowClient workflowClient = client.getWorkflowClient();
          GreetingWorkflow stub =
              workflowClient.newWorkflowStub(
                  GreetingWorkflow.class,
                  WorkflowOptions.newBuilder()
                      .setWorkflowId(getWorkflowId(input.getUserId()))
                      .setTaskQueue(HandlerWorker.TASK_QUEUE)
                      .build());

          BatchRequest request = workflowClient.newSignalWithStartRequest();
          request.add(
              stub::attachApprovalContext,
              new GreetingWorkflow.AttachApprovalContextInput(input.getNote()));
          request.add(stub::run);
          workflowClient.signalWithStart(request);

          return TemporalOperationResult.sync(null);
        });
  }
}
