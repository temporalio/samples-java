

package io.temporal.samples.sleepfordays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class SleepForDaysTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(SleepForDaysImpl.class)
          .setDoNotStart(true)
          .build();

  @Test(timeout = 8000)
  public void testSleepForDays() {
    // Mock activity
    SendEmailActivity activities = mock(SendEmailActivity.class);
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    // Start environment
    testWorkflowRule.getTestEnvironment().start();

    // Create a workflow
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    SleepForDaysWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(SleepForDaysWorkflow.class, workflowOptions);

    // Start workflow
    WorkflowClient.start(workflow::sleepForDays);

    long startTime = testWorkflowRule.getTestEnvironment().currentTimeMillis();
    // Time-skip 5 minutes.
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofMinutes(5));
    // Check that the activity has been called, we're now waiting for the sleep to finish.
    verify(activities, times(1)).sendEmail(anyString());
    // Time-skip 3 days.
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofDays(90));
    // Expect 3 more activity calls.
    verify(activities, times(4)).sendEmail(anyString());
    // Send the signal to complete the workflow.
    workflow.complete();
    // Expect no more activity calls to have been made - workflow is complete.
    verify(activities, times(4)).sendEmail(anyString());
    // Expect more than 90 days to have passed.
    long endTime = testWorkflowRule.getTestEnvironment().currentTimeMillis();
    assertEquals(true, endTime - startTime > Duration.ofDays(90).toMillis());
  }
}
