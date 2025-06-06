package io.temporal.samples.hello;

import static io.temporal.samples.hello.HelloCron.WORKFLOW_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloCron.GreetingActivities;
import io.temporal.samples.hello.HelloCron.GreetingWorkflow;
import io.temporal.samples.hello.HelloCron.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloCron}. Doesn't use an external Temporal service. */
public class HelloCronTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    // Unfortunately the supported cron format of the Java test service is not exactly the same as
    // the temporal service. For example @every is not supported by the unit testing framework.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setCronSchedule("0 * * * *")
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(WORKFLOW_ID)
            .build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution = WorkflowClient.start(workflow::greet, "World");
    assertEquals(WORKFLOW_ID, execution.getWorkflowId());
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofDays(1));
    verify(activities, atLeast(10)).greet(anyString());

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
