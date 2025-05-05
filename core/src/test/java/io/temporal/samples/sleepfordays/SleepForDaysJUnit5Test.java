

package io.temporal.samples.sleepfordays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SleepForDaysJUnit5Test {

  @RegisterExtension
  public TestWorkflowExtension testWorkflowRule =
      TestWorkflowExtension.newBuilder()
          .registerWorkflowImplementationTypes(SleepForDaysImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  @Timeout(8)
  public void testSleepForDays(
      TestWorkflowEnvironment testEnv, Worker worker, SleepForDaysWorkflow workflow) {
    // Mock activity
    SendEmailActivity activities = mock(SendEmailActivity.class);
    worker.registerActivitiesImplementations(activities);
    // Start environment
    testEnv.start();

    // Start workflow
    WorkflowClient.start(workflow::sleepForDays);

    long startTime = testEnv.currentTimeMillis();
    // Time-skip 5 minutes.
    testEnv.sleep(Duration.ofMinutes(5));
    // Check that the activity has been called, we're now waiting for the sleep to finish.
    verify(activities, times(1)).sendEmail(anyString());
    // Time-skip 3 days.
    testEnv.sleep(Duration.ofDays(90));
    // Expect 3 more activity calls.
    verify(activities, times(4)).sendEmail(anyString());
    // Send the signal to complete the workflow.
    workflow.complete();
    // Expect no more activity calls to have been made - workflow is complete.
    workflow.sleepForDays();
    verify(activities, times(4)).sendEmail(anyString());
    // Expect more than 90 days to have passed.
    long endTime = testEnv.currentTimeMillis();
    assertEquals(true, endTime - startTime > Duration.ofDays(90).toMillis());
  }
}
