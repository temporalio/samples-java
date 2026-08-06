package io.temporal.samples.resilientfanout;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates a heterogeneous fan-out with partial-failure tolerance: one critical branch and
 * several best-effort branches, run as Child Workflows in parallel.
 *
 * <p>This is different from a homogeneous fan-out (see the Fan-Out pattern in the Temporal docs),
 * where every child does the same kind of work and the run is all-or-nothing. Here the branches do
 * different things and have different failure semantics:
 *
 * <ul>
 *   <li>If the critical branch ({@link CreateAccountWorkflow}) fails, the whole run fails, and the
 *       still-running best-effort branches are cancelled instead of being left to finish and later
 *       fail trying to report results to an already-dead parent.
 *   <li>If a best-effort branch fails on its own, the run still succeeds, just in a degraded state
 *       that callers can inspect via {@link OnboardingResult#getDegradedReasons()}.
 * </ul>
 */
public class OnboardingWorkflowImpl implements OnboardingWorkflow {

  @Override
  public OnboardingResult onboard(String userId) {
    String parentId = Workflow.getInfo().getWorkflowId();

    CreateAccountWorkflow createAccountWorkflow =
        Workflow.newChildWorkflowStub(
            CreateAccountWorkflow.class, childOptions(parentId + "/create-account"));
    Promise<String> accountPromise = Async.function(createAccountWorkflow::createAccount, userId);

    // Started inside a CancellationScope so a critical-branch failure can tear them down.
    List<Promise<Void>> bestEffortResults = new ArrayList<>();
    List<String> bestEffortNames = new ArrayList<>();
    CancellationScope bestEffortScope =
        Workflow.newCancellationScope(
            () -> {
              ProvisionStorageWorkflow storageWorkflow =
                  Workflow.newChildWorkflowStub(
                      ProvisionStorageWorkflow.class,
                      childOptions(parentId + "/provision-storage"));
              bestEffortNames.add("provision-storage");
              bestEffortResults.add(Async.procedure(storageWorkflow::provisionStorage, userId));

              SendWelcomeEmailWorkflow emailWorkflow =
                  Workflow.newChildWorkflowStub(
                      SendWelcomeEmailWorkflow.class,
                      childOptions(parentId + "/send-welcome-email"));
              bestEffortNames.add("send-welcome-email");
              bestEffortResults.add(Async.procedure(emailWorkflow::sendWelcomeEmail, userId));
            });
    bestEffortScope.run();

    String accountId;
    try {
      // Awaited on its own, not via Promise.allOf(...), so it can't be failed by a sibling.
      accountId = accountPromise.get();
    } catch (ChildWorkflowFailure e) {
      // Cancel explicitly rather than relying on the default ParentClosePolicy (TERMINATE).
      bestEffortScope.cancel();
      reconcileFailure(userId, e.getMessage());
      throw e;
    }

    // A best-effort failure here degrades the result instead of failing the run.
    List<String> degradedReasons = new ArrayList<>();
    for (int i = 0; i < bestEffortResults.size(); i++) {
      try {
        bestEffortResults.get(i).get();
      } catch (ChildWorkflowFailure e) {
        degradedReasons.add(bestEffortNames.get(i) + ": " + e.getMessage());
      }
    }
    return new OnboardingResult(accountId, degradedReasons);
  }

  /**
   * Independent cleanup for a failed run: notify support, record an audit entry, and release the
   * billing hold. These three calls hit unrelated systems and must not block each other -- one
   * failing should not prevent the other two from being attempted. {@code Saga} with parallel
   * compensation gives us that isolation for free, even though this isn't classic Saga "undo what I
   * did" semantics: there's nothing to undo, only independent notifications to fire.
   */
  private void reconcileFailure(String userId, String reason) {
    ReconciliationActivities activities =
        Workflow.newActivityStub(
            ReconciliationActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(true).build());
    saga.addCompensation(activities::notifySupportTeam, userId, reason);
    saga.addCompensation(activities::recordFailedOnboarding, userId, reason);
    saga.addCompensation(activities::releaseBillingHold, userId);

    // Detached so reconciliation still completes while this workflow is unwinding.
    Workflow.newDetachedCancellationScope(saga::compensate).run();
  }

  private ChildWorkflowOptions childOptions(String workflowId) {
    return ChildWorkflowOptions.newBuilder().setWorkflowId(workflowId).build();
  }
}
