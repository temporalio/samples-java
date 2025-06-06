package io.temporal.samples.excludefrominterceptor;

import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.samples.excludefrominterceptor.activities.ForInterceptorActivitiesImpl;
import io.temporal.samples.excludefrominterceptor.activities.MyActivitiesImpl;
import io.temporal.samples.excludefrominterceptor.interceptor.MyWorkerInterceptor;
import io.temporal.samples.excludefrominterceptor.workflows.*;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ExcludeFromInterceptorTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(MyWorkflowOneImpl.class, MyWorkflowTwoImpl.class)
          .setActivityImplementations(new MyActivitiesImpl(), new ForInterceptorActivitiesImpl())
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(
                      new MyWorkerInterceptor(
                          // exclude MyWorkflowTwo from workflow interceptors
                          Arrays.asList(MyWorkflowTwo.class.getSimpleName()),
                          // exclude ActivityTwo and the "ForInterceptor" activities from activity
                          // interceptor
                          // note with SpringBoot starter you could use bean names here, we use
                          // strings to
                          // not have
                          // to reflect on the activity impl class in sample
                          Arrays.asList(
                              "ActivityTwo",
                              "ForInterceptorActivityOne",
                              "ForInterceptorActivityTwo")))
                  .build())
          .build();

  @Test
  public void testExcludeFromInterceptor() {
    MyWorkflow myWorkflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                MyWorkflowOne.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("MyWorkflowOne")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    MyWorkflowTwo myWorkflowTwo =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                MyWorkflowTwo.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("MyWorkflowTwo")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    WorkflowClient.start(myWorkflow::execute, "my workflow input");
    WorkflowClient.start(myWorkflowTwo::execute, "my workflow two input");

    // wait for both execs to complete
    try {
      CompletableFuture.allOf(
              WorkflowStub.fromTyped(myWorkflow).getResultAsync(String.class),
              WorkflowStub.fromTyped(myWorkflowTwo).getResultAsync(String.class))
          .get();
    } catch (Exception e) {
      Assert.fail("Exception executing workflows: " + e.getMessage());
    }

    Assert.assertEquals(5, getNumOfActivitiesForExec("MyWorkflowOne"));
    Assert.assertEquals(2, getNumOfActivitiesForExec("MyWorkflowTwo"));
  }

  private int getNumOfActivitiesForExec(String workflowId) {
    WorkflowExecutionHistory history =
        testWorkflowRule.getWorkflowClient().fetchHistory(workflowId);
    int counter = 0;
    for (HistoryEvent event : history.getEvents()) {
      if (event.getEventType().equals(EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED)) {
        counter++;
      }
    }
    return counter;
  }
}
