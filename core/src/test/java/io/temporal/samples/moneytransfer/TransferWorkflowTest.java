package io.temporal.samples.moneytransfer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class TransferWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(AccountTransferWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testTransfer() {
    Account activities = mock(Account.class);
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);

    testWorkflowRule.getTestEnvironment().start();

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    AccountTransferWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(AccountTransferWorkflow.class, options);

    long start = testWorkflowRule.getTestEnvironment().currentTimeMillis();
    workflow.transfer("account1", "account2", "reference1", 123);
    long duration = testWorkflowRule.getTestEnvironment().currentTimeMillis() - start;
    System.out.println("Duration hours: " + duration / 3600000);

    verify(activities).withdraw(eq("account1"), eq("reference1"), eq(123));
    verify(activities).deposit(eq("account2"), eq("reference1"), eq(123));

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
