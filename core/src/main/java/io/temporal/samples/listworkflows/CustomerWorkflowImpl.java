

package io.temporal.samples.listworkflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Optional;

public class CustomerWorkflowImpl implements CustomerWorkflow {
  private boolean exit;
  private final CustomerActivities customerActivities =
      Workflow.newActivityStub(
          CustomerActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
  private final RetryOptions customerRetryOptions =
      RetryOptions.newBuilder().setMaximumAttempts(5).build();
  private final Duration expiration = Duration.ofMinutes(1);

  @Override
  public void updateAccountMessage(Customer customer, String message) {

    Workflow.retry(
        customerRetryOptions,
        Optional.of(expiration),
        () -> {
          customerActivities.getCustomerAccount(customer);
          customerActivities.updateCustomerAccount(customer, message);
          customerActivities.sendUpdateEmail(customer);
        });

    Workflow.await(Duration.ofMinutes(1), () -> exit);
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
