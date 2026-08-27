package io.temporal.samples.nexuswalkthrough.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.BatchRequest;
import io.temporal.client.StartActivityOptions;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.common.RetryOptions;
import io.temporal.nexus.TemporalOperationHandler;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.samples.nexuswalkthrough.generatedservice.ApprovalService;
import io.temporal.samples.nexuswalkthrough.generatedservice.AttachApprovalContextInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.CheckApprovalRequiredInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.CheckApprovalRequiredOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RemindApproverInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionOutput;
import java.time.Duration;

/**
 * The handler side of the approval Service. Every Operation in the contract is implemented here,
 * and each one demonstrates a different Nexus capability from the walkthrough.
 *
 * <p>Read this file top to bottom alongside the walkthrough - the Operations appear in the order
 * the walkthrough introduces them:
 *
 * <ul>
 *   <li>STEP 3 - checkApprovalRequired, a synchronous Operation with no backing Execution
 *   <li>STEP 4 - requestApproval, backed by a Workflow
 *   <li>STEP 7 - remindApprover, a Signal delivered as sync messaging
 *   <li>STEP 7 - submitDecision, backed by a Workflow Update
 *   <li>STEP 8 - attachApprovalContext, Signal-with-Start
 *   <li>STEP 9 - notifyRequester, backed by a Standalone Activity
 * </ul>
 *
 * <p>Every one of them uses {@link TemporalOperationHandler}, including the simplest. Starting with
 * it means an Operation can later gain a message or change its backing without changing shape.
 */
@ServiceImpl(service = ApprovalService.class)
public class ApprovalServiceImpl {

  /** The spend threshold applied by checkApprovalRequired. Below this, no approval is needed. */
  private static final double APPROVAL_THRESHOLD = 500.00;

  // ===============================================================================================
  // STEP 3 - An Operation with no backing Execution.
  //
  // The handler computes an answer and returns it. Nothing durable is created: no Workflow, no
  // Activity, nothing to cancel, nothing in Event History. The Operation completes during the
  // handler call and the caller gets the answer in the response.
  //
  // This fits work that cannot meaningfully fail and returns immediately. Compare it against
  // notifyRequester at the bottom of this file: both are "one small thing", and they get opposite
  // answers. Sending a notification can fail and you want that retried with a record of each
  // attempt, so it needs an Activity. Comparing an amount to a threshold cannot fail in any way
  // worth retrying, so an Activity Execution would be pure overhead.
  //
  // Note this still runs inside the Nexus handler call, so it is bounded by the handler deadline
  // of under 10 seconds. That is plenty for a threshold comparison and would not be for anything
  // that talks to a slow dependency.
  // ===============================================================================================
  // @@@SNIPSTART samples-java-nexus-walkthrough-check-approval-required
  @OperationImpl
  public OperationHandler<CheckApprovalRequiredInput, CheckApprovalRequiredOutput>
      checkApprovalRequired() {
    return TemporalOperationHandler.create(
        (ctx, client, input) ->
            TemporalOperationResult.sync(
                new CheckApprovalRequiredOutput(
                    input.getAmount() >= APPROVAL_THRESHOLD, APPROVAL_THRESHOLD)));
  }

  // @@@SNIPEND

  // ===============================================================================================
  // STEP 4 - A Workflow-backed Operation, with the STEP 8 conflict policy applied.
  //
  // Calling startWorkflow on the injected client starts the approval Workflow and attaches this
  // Operation's completion callback to it. The Operation then completes when the Workflow returns,
  // delivering the Workflow's return value to the caller.
  //
  // The Workflow Id comes from the item id (see ApprovalWorkflowId), not from a random value, so
  // that later messages can find this Execution.
  //
  // By default, starting a Workflow whose Id is already running FAILS the Operation. That default
  // is deliberate: the Operation has only started successfully once its completion callback is
  // attached to a Workflow, so failing loudly beats reporting success to a caller that would then
  // wait forever.
  //
  // Here that default is replaced with USE_EXISTING, which step 8 explains. Once
  // attachApprovalContext can create the approval first, this Operation needs to attach to the
  // running Execution instead of failing. Two things follow: more than one caller can await the
  // same approval, and the Operation becomes idempotent for genuinely separate callers rather than
  // only for server retries of one request.
  // ===============================================================================================
  // @@@SNIPSTART samples-java-nexus-walkthrough-request-approval
  @OperationImpl
  public OperationHandler<RequestApprovalInput, RequestApprovalOutput> requestApproval() {
    return TemporalOperationHandler.create(
        (ctx, client, input) ->
            client.startWorkflow(
                ApprovalWorkflow.class,
                ApprovalWorkflow::runApproval,
                input,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(ApprovalWorkflowId.forItem(input.getItemId()))
                    .setWorkflowIdConflictPolicy(
                        WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                    .build()));
  }

  // @@@SNIPEND

  // ===============================================================================================
  // STEP 7 - A Signal, delivered as sync messaging.
  //
  // Send the Signal through the client, then return a synchronous result. The Operation completes
  // immediately, during the handler call - there is no completion callback and nothing to await,
  // because a Signal is fire-and-forget.
  //
  // The whole handler call has to finish inside the handler deadline of under 10 seconds, and the
  // budget is smaller than that because the clock starts on the caller's side and the request still
  // routes through matching. One Signal is comfortably inside it.
  //
  // This targets a Workflow that already exists. The Temporal Service accepts a Signal only while
  // the Workflow is still running, so a remindApprover for a purchase whose approval has already
  // been decided fails with NOT_FOUND: workflow execution already completed, and one for a
  // purchase that never had an approval fails as not found.
  // ===============================================================================================
  // @@@SNIPSTART samples-java-nexus-walkthrough-remind-approver
  @OperationImpl
  public OperationHandler<RemindApproverInput, Void> remindApprover() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          client
              .getWorkflowClient()
              .newWorkflowStub(
                  ApprovalWorkflow.class, ApprovalWorkflowId.forItem(input.getItemId()))
              .remindApprover();
          return TemporalOperationResult.sync(null);
        });
  }

  // @@@SNIPEND

  // ===============================================================================================
  // STEP 7 - An Update-backed Operation.
  //
  // The caller needs a result back - confirmation that the decision was recorded - which is what
  // makes this an Update rather than a Signal.
  //
  // This is an async backing: the Operation completes when the Update completes, and its result is
  // delivered through the Nexus completion callback. If the Update happens to come back already
  // complete, the result returns synchronously instead.
  //
  // Two requirements follow. It targets a Workflow that already exists, so a submitDecision for a
  // purchase with no approval running fails. And because it is an async backing, there is at most
  // one per Operation invocation, though a handler could still combine it with sync side effects.
  // ===============================================================================================
  // @@@SNIPSTART samples-java-nexus-walkthrough-submit-decision
  @OperationImpl
  public OperationHandler<SubmitDecisionInput, SubmitDecisionOutput> submitDecision() {
    return TemporalOperationHandler.create(
        (ctx, client, input) ->
            client.startWorkflowUpdate(
                ApprovalWorkflow.class,
                ApprovalWorkflowId.forItem(input.getItemId()),
                ApprovalWorkflow::submitDecision,
                input.getDecision(),
                UpdateOptions.<SubmitDecisionOutput>newBuilder()
                    .setResultClass(SubmitDecisionOutput.class)
                    // The Update to invoke has to be named explicitly; the method reference above
                    // supplies the argument types but not the wire name.
                    .setUpdateName(ApprovalWorkflow.SUBMIT_DECISION_UPDATE)
                    // An Update-backed Operation must wait for the ACCEPTED stage. The Operation
                    // completes later, when the Update completes, through the completion callback.
                    // Any other stage is rejected with "nexus op workflow updates only support
                    // WorkflowUpdateStageAccepted for async updates".
                    .setWaitForStage(WorkflowUpdateStage.ACCEPTED)
                    .build()));
  }

  // @@@SNIPEND

  // ===============================================================================================
  // STEP 8 - Signal-with-Start.
  //
  // Supporting information is produced by a different system than the one requesting approval, and
  // the two messages can arrive in either order. This Operation is written so that either order
  // works, which means it may have to start the approval itself.
  //
  // That is why its input repeats the purchase details rather than just naming an approval: an
  // Operation that can create the thing it messages has to carry enough to create it.
  //
  // Both this and requestApproval derive the same Workflow Id from the same item id, which is what
  // lets them agree on which Execution they mean regardless of which one arrives first.
  //
  // Like remindApprover this is sync messaging, so it completes during the handler call.
  // ===============================================================================================
  // @@@SNIPSTART samples-java-nexus-walkthrough-attach-context
  @OperationImpl
  public OperationHandler<AttachApprovalContextInput, Void> attachApprovalContext() {
    return TemporalOperationHandler.create(
        (ctx, client, input) -> {
          WorkflowClient workflowClient = client.getWorkflowClient();
          ApprovalWorkflow stub =
              workflowClient.newWorkflowStub(
                  ApprovalWorkflow.class,
                  WorkflowOptions.newBuilder()
                      .setWorkflowId(ApprovalWorkflowId.forItem(input.getItemId()))
                      .setTaskQueue(HandlerWorker.DEFAULT_TASK_QUEUE_NAME)
                      .build());

          // signalWithStart delivers the Signal, starting the Workflow first if it is not already
          // running. When the approval already exists, only the Signal is delivered.
          BatchRequest request = workflowClient.newSignalWithStartRequest();
          request.add(stub::attachContext, input.getNote());
          request.add(
              stub::runApproval,
              new RequestApprovalInput(input.getItemId(), input.getRequester(), input.getAmount()));
          workflowClient.signalWithStart(request);

          return TemporalOperationResult.sync(null);
        });
  }

  // @@@SNIPEND

  // ===============================================================================================
  // STEP 9 - An Activity-backed Operation, using a Standalone Activity.
  //
  // Use TemporalOperationHandler as with every other Operation, but start an Activity instead of a
  // Workflow. The Operation starts an Activity Execution with no parent Workflow and completes when
  // the Activity returns.
  //
  // This is the right shape whenever an Operation is one durable step behind a team boundary. The
  // Activity supplies the durability - retries on the policy you set, timeouts you control, and a
  // record of every attempt - and the Operation supplies the contract, so the notification is
  // reachable by other teams without them sharing your code or your Namespace.
  //
  // Before Activity-backed Operations this would have needed a Workflow whose only job was to call
  // one Activity: a wrapper with its own Event History and Workflow Id, providing nothing.
  //
  // An Activity-backed Operation requires an Activity Id, unique within the Namespace, because
  // there is no parent Workflow to scope it. The Task Queue set below does not have to be the
  // Endpoint's target Task Queue - notifications could run on their own Worker fleet - but this
  // sample keeps them on one Worker for simplicity.
  //
  // Note the contrast with checkApprovalRequired at the top of this file. Both are one small thing.
  // This one touches the outside world and can fail, so it needs an Activity rather than nothing.
  // ===============================================================================================
  // @@@SNIPSTART samples-java-nexus-walkthrough-notify-requester
  @OperationImpl
  public OperationHandler<NotifyRequesterInput, NotifyRequesterOutput> notifyRequester() {
    return TemporalOperationHandler.create(
        (ctx, client, input) ->
            client.startActivity(
                ApprovalActivities.class,
                ApprovalActivities::notifyRequester,
                input.getRequester(),
                input.getDecision(),
                StartActivityOptions.newBuilder()
                    // Deriving the Activity Id from the request Id keeps a retried Nexus start
                    // request targeting the same Activity Execution instead of sending a second
                    // notification.
                    .setId("notify-" + ctx.getRequestId())
                    .setTaskQueue(HandlerWorker.DEFAULT_TASK_QUEUE_NAME)
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build()));
  }
  // @@@SNIPEND
}
