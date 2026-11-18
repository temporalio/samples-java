package io.temporal.samples.moneybatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowTargetOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowRule;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

public class TransferWorkflowTest {
  private static final String WORKFLOW_ID = "moneybatch-749-reproduction";

  @Rule
  public TestWorkflowRule rule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(AccountTransferWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test(timeout = 30000)
  public void testTransfer() throws Exception {
    Account activities = mock(Account.class);
    rule.getWorker().registerActivitiesImplementations(activities);
    rule.getTestEnvironment().start();

    String from = "account1";
    String to = "account2";
    int batchSize = 5;
    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(rule.getTaskQueue()).setWorkflowId(to).build();
    AccountTransferWorkflow transferWorkflow =
        rule.getWorkflowClient().newWorkflowStub(AccountTransferWorkflow.class, options);
    WorkflowClient.start(transferWorkflow::deposit, to, batchSize, new BatchState());
    Random random = new Random();
    int total = 0;
    for (int i = 0; i < batchSize; i++) {
      int amountCents = random.nextInt(1000);
      transferWorkflow.withdraw(from, UUID.randomUUID().toString(), amountCents);
      total += amountCents;
    }
    // Wait for workflow to finish
    WorkflowStub.fromTyped(transferWorkflow).getResult(5, TimeUnit.SECONDS, Void.class);
    verify(activities).deposit(eq("account2"), any(), eq(total));

    rule.getTestEnvironment().shutdown();
  }

  @Test(timeout = 30000)
  public void carriesSixthRequestWithoutWithdrawingItUntilNextBatchIsFull() throws Exception {
    RecordingAccount account = new RecordingAccount(false);
    AccountTransferWorkflow workflow = newWorkflow(account);
    WorkflowExecution first = start(workflow);
    // Buffer all six signals before polling starts, so one task sees more than a batch.
    send(workflow, 0, 6);
    rule.getTestEnvironment().start();
    String nextRun = awaitContinuedRun(first.getRunId());

    assertEquals(List.of(1500), account.deposits);
    assertEquals(5, account.withdrawals.size());
    assertEquals(
        Set.of("request-0", "request-1", "request-2", "request-3", "request-4"),
        new HashSet<>(account.withdrawals));
    assertFalse(account.withdrawals.contains("request-5"));
    assertEquals(0, workflow.getCount());
    assertEquals(0, workflow.getBalance());

    // Both already-settled and carried references must remain deduplicated across runs.
    workflow.withdraw("source", "request-0", 99999);
    workflow.withdraw("source", "request-5", 99999);
    send(workflow, 6, 10);
    finish(workflow);

    assertEquals(List.of(1500, 4000), account.deposits);
    assertEquals(10, account.withdrawals.size());
    assertEquals(10, new HashSet<>(account.withdrawals).size());
    assertRunCompleted(nextRun);
  }

  @Test(timeout = 30000)
  public void preservesSignalsReceivedWhileDepositIsBlocked() throws Exception {
    RecordingAccount account = new RecordingAccount(true);
    AccountTransferWorkflow workflow = newWorkflow(account);
    WorkflowExecution first = start(workflow);
    send(workflow, 0, 5);
    rule.getTestEnvironment().start();
    try {
      assertTrue(account.firstDeposit.await(5, TimeUnit.SECONDS));
      send(workflow, 5, 7);
      workflow.withdraw("source", "request-0", 99999);
      assertEquals(5, workflow.getCount());
      assertEquals(1500, workflow.getBalance());
      assertEquals(5, account.withdrawals.size());
    } finally {
      account.releaseDeposit.countDown();
    }
    String nextRun = awaitContinuedRun(first.getRunId());
    assertEquals(0, workflow.getCount());
    assertEquals(5, account.withdrawals.size());
    send(workflow, 7, 10);
    finish(workflow);

    assertEquals(List.of(1500, 4000), account.deposits);
    assertEquals(10, new HashSet<>(account.withdrawals).size());
    assertRunCompleted(nextRun);
  }

  @Test(timeout = 30000)
  public void propagatesWithdrawalFailureInsteadOfWaitingForeverForCount() throws Exception {
    RecordingAccount account =
        new RecordingAccount(false) {
          @Override
          public synchronized void withdraw(String accountId, String referenceId, int amountCents) {
            throw ApplicationFailure.newNonRetryableFailure("withdrawal rejected", "Rejected");
          }
        };
    AccountTransferWorkflow workflow = newWorkflow(account);
    start(workflow);
    send(workflow, 0, 5);
    rule.getTestEnvironment().start();
    assertThrows(WorkflowFailedException.class, () -> finish(workflow));
    assertTrue(account.deposits.isEmpty());
  }

  private AccountTransferWorkflow newWorkflow(Account account) {
    rule.getWorker().registerActivitiesImplementations(account);
    return rule.getWorkflowClient()
        .newWorkflowStub(
            AccountTransferWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(rule.getTaskQueue())
                .build());
  }

  private WorkflowExecution start(AccountTransferWorkflow workflow) {
    return WorkflowClient.start(workflow::deposit, "destination", 5, new BatchState());
  }

  private void send(AccountTransferWorkflow workflow, int from, int to) {
    for (int i = from; i < to; i++) {
      workflow.withdraw("source", "request-" + i, (i + 1) * 100);
    }
  }

  private void finish(AccountTransferWorkflow workflow) throws Exception {
    WorkflowStub.fromTyped(workflow).getResult(5, TimeUnit.SECONDS, Void.class);
  }

  private String awaitContinuedRun(String runId) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    do {
      for (HistoryEvent event :
          rule.getWorkflowClient().fetchHistory(WORKFLOW_ID, runId).getEvents()) {
        if (event.getEventType() == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_CONTINUED_AS_NEW) {
          return event.getWorkflowExecutionContinuedAsNewEventAttributes().getNewExecutionRunId();
        }
      }
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    throw new AssertionError("Expected Continue-As-New for run " + runId);
  }

  private void assertRunCompleted(String runId) {
    List<HistoryEvent> events =
        rule.getWorkflowClient().fetchHistory(WORKFLOW_ID, runId).getEvents();
    assertEquals(
        EventType.EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED,
        events.get(events.size() - 1).getEventType());
    assertEquals(
        5,
        rule.getWorkflowClient()
            .newWorkflowStub(
                AccountTransferWorkflow.class,
                WorkflowTargetOptions.newBuilder()
                    .setWorkflowId(WORKFLOW_ID)
                    .setRunId(runId)
                    .build())
            .getCount());
  }

  private static class RecordingAccount implements Account {
    final List<String> withdrawals = new CopyOnWriteArrayList<>();
    final List<Integer> deposits = new CopyOnWriteArrayList<>();
    final CountDownLatch firstDeposit = new CountDownLatch(1);
    final CountDownLatch releaseDeposit;

    RecordingAccount(boolean blockDeposit) {
      releaseDeposit = new CountDownLatch(blockDeposit ? 1 : 0);
    }

    @Override
    public synchronized void withdraw(String accountId, String referenceId, int amountCents) {
      withdrawals.add(referenceId);
    }

    @Override
    public void deposit(String accountId, String referenceId, int amountCents) {
      deposits.add(amountCents);
      firstDeposit.countDown();
      try {
        if (!releaseDeposit.await(4, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Deposit was not released");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
  }
}
