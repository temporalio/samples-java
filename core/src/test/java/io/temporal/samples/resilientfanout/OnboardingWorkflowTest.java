package io.temporal.samples.resilientfanout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OnboardingWorkflowTest {

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(
              OnboardingWorkflowImpl.class,
              CreateAccountWorkflowImpl.class,
              ProvisionStorageWorkflowImpl.class,
              SendWelcomeEmailWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testHappyPath(
      TestWorkflowEnvironment testEnv, Worker worker, OnboardingWorkflow workflow) {
    OnboardingActivities activities = mock(OnboardingActivities.class);
    when(activities.createAccount("user-1")).thenReturn("account-user-1");
    ReconciliationActivities reconciliation = mock(ReconciliationActivities.class);
    worker.registerActivitiesImplementations(activities, reconciliation);
    testEnv.start();

    OnboardingResult result = workflow.onboard("user-1");

    assertEquals("account-user-1", result.getAccountId());
    assertFalse(result.isDegraded());
    verify(reconciliation, never()).notifySupportTeam(any(), any());
  }

  @Test
  public void testBestEffortFailureDegradesButSucceeds(
      TestWorkflowEnvironment testEnv, Worker worker, OnboardingWorkflow workflow) {
    OnboardingActivities activities = mock(OnboardingActivities.class);
    when(activities.createAccount("user-degraded")).thenReturn("account-user-degraded");
    doThrow(
            ApplicationFailure.newNonRetryableFailure(
                "storage quota service unavailable", "StorageProvisioningFailure"))
        .when(activities)
        .provisionStorage("user-degraded");
    ReconciliationActivities reconciliation = mock(ReconciliationActivities.class);
    worker.registerActivitiesImplementations(activities, reconciliation);
    testEnv.start();

    OnboardingResult result = workflow.onboard("user-degraded");

    assertEquals("account-user-degraded", result.getAccountId());
    assertTrue(result.isDegraded());
    assertTrue(result.getDegradedReasons().get(0).startsWith("provision-storage:"));
    // The critical branch succeeded, so this was never a failure -- reconciliation must not run.
    verify(reconciliation, never()).notifySupportTeam(any(), any());
  }

  @Test
  public void testCriticalFailureCancelsSiblingsAndReconciles(
      TestWorkflowEnvironment testEnv, Worker worker, OnboardingWorkflow workflow) {
    OnboardingActivities activities = mock(OnboardingActivities.class);
    when(activities.createAccount("user-critical-failure"))
        .thenThrow(
            ApplicationFailure.newNonRetryableFailure(
                "account service rejected the request", "AccountCreationFailure"));
    ReconciliationActivities reconciliation = mock(ReconciliationActivities.class);
    worker.registerActivitiesImplementations(activities, reconciliation);
    testEnv.start();

    assertThrows(WorkflowException.class, () -> workflow.onboard("user-critical-failure"));

    // All three reconciliation calls are independent of each other -- verify each was attempted
    // exactly once, which is the behavior Saga's parallel compensation gives us here.
    verify(reconciliation, times(1)).notifySupportTeam(eq("user-critical-failure"), any());
    verify(reconciliation, times(1)).recordFailedOnboarding(eq("user-critical-failure"), any());
    verify(reconciliation, times(1)).releaseBillingHold(eq("user-critical-failure"));
  }
}
