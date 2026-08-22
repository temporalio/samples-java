package io.temporal.samples.nexuswalkthrough.caller;

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
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * WALKTHROUGH STEP 6, 8 and 10 - Calling the Service.
 *
 * <p>The caller knows two things: the Endpoint name and the contract. It does not know which
 * Namespace the handler runs in, which Task Queue its Worker polls, or that requestApproval is
 * backed by a Workflow while checkApprovalRequired is backed by nothing at all.
 *
 * <p>That is the property worth pausing on: the handler team can change what backs an Operation,
 * move the handler to another Namespace, or rewrite it in another language, and this caller keeps
 * working.
 */
// @@@SNIPSTART samples-java-nexus-walkthrough-caller-workflow
public class ApprovalCallerWorkflowImpl implements ApprovalCallerWorkflow {

  private static final Logger logger = Workflow.getLogger(ApprovalCallerWorkflowImpl.class);

  // STEP 6 - In Java the Service interface works directly as a Nexus Service stub. Because the stub
  // is that interface, every call below is type-checked against the contract at compile time.
  //
  // The schedule-to-close timeout bounds the whole Operation. A human approval measured in days
  // would need a timeout in days; this sample decides in seconds, so a short one is fine. The
  // default would not be right for a real approval.
  private final ApprovalService approvalService =
      Workflow.newNexusServiceStub(
          ApprovalService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofMinutes(2))
                      .build())
              .build());

  @Override
  public String runApprovalFlow(String itemId, String requester, double amount, String note) {

    // -------------------------------------------------------------------------------------------
    // STEP 6 - A synchronous Operation. It returns during the call because nothing durable backs
    // it: no callback, no Operation token, nothing to await. A caller can use it to skip the rest
    // of this Service entirely.
    // -------------------------------------------------------------------------------------------
    CheckApprovalRequiredOutput check =
        approvalService.checkApprovalRequired(
            new CheckApprovalRequiredInput(itemId, requester, amount));

    logger.info(
        "checkApprovalRequired -> required={} threshold={}",
        check.getApprovalRequired(),
        check.getThreshold());

    if (!check.getApprovalRequired()) {
      return "NO_APPROVAL_REQUIRED";
    }

    // -------------------------------------------------------------------------------------------
    // STEP 8 - Attach information before the approval exists.
    //
    // This is deliberately called BEFORE requestApproval, which is the harder ordering. Because
    // attachApprovalContext is Signal-with-Start, this call creates the approval Workflow and
    // delivers the note to it.
    // -------------------------------------------------------------------------------------------
    approvalService.attachApprovalContext(
        new AttachApprovalContextInput(itemId, requester, amount, note));
    logger.info("attachApprovalContext -> note attached, approval now exists");

    // -------------------------------------------------------------------------------------------
    // STEP 6 - Request the approval.
    //
    // The approval Workflow is already running thanks to the call above, so this start would fail
    // under the default conflict policy. The handler sets USE_EXISTING, so instead this attaches
    // the Operation's completion callback to the running Execution.
    //
    // startNexusOperation returns a handle rather than blocking, so this Workflow can keep working
    // while the approval is pending. The wait is durable: this caller can be evicted and its Worker
    // can restart, and the result still arrives.
    // -------------------------------------------------------------------------------------------
    NexusOperationHandle<RequestApprovalOutput> approvalHandle =
        Workflow.startNexusOperation(
            approvalService::requestApproval, new RequestApprovalInput(itemId, requester, amount));

    // Wait for the Operation to be started before messaging it. NexusOperationExecution carries the
    // Operation token for an asynchronous Operation.
    approvalHandle.getExecution().get();
    logger.info("requestApproval -> started and attached to the existing approval");

    // -------------------------------------------------------------------------------------------
    // STEP 8 - Nudge the pending approval. A Signal, so there is no result to collect.
    // -------------------------------------------------------------------------------------------
    approvalService.remindApprover(new RemindApproverInput(itemId));
    logger.info("remindApprover -> approver nudged");

    // -------------------------------------------------------------------------------------------
    // STEP 8 - Submit the decision. An Update, so the caller gets confirmation back.
    //
    // In a real system this arrives from a human through a separate caller. The sample submits it
    // here so the flow completes without one.
    // -------------------------------------------------------------------------------------------
    SubmitDecisionOutput ack =
        approvalService.submitDecision(
            new SubmitDecisionInput(itemId, SubmitDecisionInput.Decision.DECISION_APPROVED));
    logger.info(
        "submitDecision -> recorded={} after {} reminder(s)",
        ack.getRecorded().getValue(),
        ack.getRemindersSent());

    // -------------------------------------------------------------------------------------------
    // STEP 6 - Await the decision.
    //
    // The caller does not poll. The decision is the result of requestApproval, pushed here through
    // the Nexus completion callback the moment the approval Workflow returns. Asking the approval
    // for its status in a loop would be polling for something already on its way.
    // -------------------------------------------------------------------------------------------
    RequestApprovalOutput.Decision decision = approvalHandle.getResult().get().getDecision();
    logger.info("requestApproval -> decision {}", decision.getValue());

    // -------------------------------------------------------------------------------------------
    // STEP 10 - Call the Standalone Activity.
    //
    // From the caller this looks like any other Operation. It does not know that nothing but a
    // single Activity Execution sits behind it.
    // -------------------------------------------------------------------------------------------
    NotifyRequesterOutput notified =
        approvalService.notifyRequester(
            new NotifyRequesterInput(
                requester, NotifyRequesterInput.Decision.fromString(decision.getValue())));
    logger.info("notifyRequester -> delivered to {}", notified.getDeliveredTo());

    return decision.getValue();
  }
}
// @@@SNIPEND
