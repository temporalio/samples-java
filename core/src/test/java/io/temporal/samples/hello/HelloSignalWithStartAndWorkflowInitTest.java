package io.temporal.samples.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class HelloSignalWithStartAndWorkflowInitTest {
  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .registerWorkflowImplementationTypes(
              HelloSignalWithStartAndWorkflowInit.WithInitMyWorkflowImpl.class)
          .registerWorkflowImplementationTypes(
              WorkflowImplementationOptions.newBuilder()
                  .setFailWorkflowExceptionTypes(NullPointerException.class)
                  .build(),
              HelloSignalWithStartAndWorkflowInit.WithoutInitMyWorkflowImpl.class)
          .setActivityImplementations(
              new HelloSignalWithStartAndWorkflowInit.MyGreetingActivitiesImpl())
          .build();

  @Test
  public void testWithInit(TestWorkflowEnvironment testEnv, Worker worker) {
    HelloSignalWithStartAndWorkflowInit.MyWorkflowWithInit withInitStub =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                HelloSignalWithStartAndWorkflowInit.MyWorkflowWithInit.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("with-init")
                    .setTaskQueue(worker.getTaskQueue())
                    .build());
    WorkflowStub.fromTyped(withInitStub)
        .signalWithStart(
            "addGreeting",
            new Object[] {new HelloSignalWithStartAndWorkflowInit.Person("Michael", "Jordan", 55)},
            new Object[] {new HelloSignalWithStartAndWorkflowInit.Person("John", "Stockton", 57)});
    String result = WorkflowStub.fromTyped(withInitStub).getResult(String.class);
    assertEquals("Hello Michael Jordan,Hello John Stockton", result);
  }

  @Test
  public void testWithoutInit(TestWorkflowEnvironment testEnv, Worker worker) {
    HelloSignalWithStartAndWorkflowInit.MyWorkflowNoInit noInitStub =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                HelloSignalWithStartAndWorkflowInit.MyWorkflowNoInit.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("without-init")
                    .setTaskQueue(worker.getTaskQueue())
                    .build());
    WorkflowStub.fromTyped(noInitStub)
        .signalWithStart(
            "addGreeting",
            new Object[] {new HelloSignalWithStartAndWorkflowInit.Person("Michael", "Jordan", 55)},
            new Object[] {new HelloSignalWithStartAndWorkflowInit.Person("John", "Stockton", 57)});
    try {
      WorkflowStub.fromTyped(noInitStub).getResult(String.class);
      fail("Workflow execution should have failed");
    } catch (Exception e) {
      if (!(e instanceof WorkflowFailedException)) {
        fail("Workflow execution should have failed with WorkflowFailedException");
      }
    }
  }
}
