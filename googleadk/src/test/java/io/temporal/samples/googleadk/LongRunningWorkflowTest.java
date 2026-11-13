package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.CanceledFailure;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.longrunning.LongRunningWorkflow;
import io.temporal.samples.googleadk.longrunning.LongRunningWorkflowImpl;
import io.temporal.samples.googleadk.longrunning.ReportActivities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class LongRunningWorkflowTest {
  private static ScriptedModel model() {
    return ScriptedModel.responding(
        AgentSupport.MODEL,
        request ->
            AdkTestEnvironment.functionResponse(request, "startReport")
                .map(
                    response ->
                        ScriptedModel.text(
                            "done".equals(response.get("status"))
                                ? "Report complete: " + response.get("result")
                                : "Report is pending."))
                .orElseGet(
                    () -> ScriptedModel.functionCall("startReport", Map.of("name", "sales"))));
  }

  @Test
  void callbackResumesTheOriginalToolCallWithoutSubmittingAnotherJob() throws Exception {
    ScriptedModel model = model();
    AtomicInteger submissions = new AtomicInteger();
    ReportActivities reports =
        name -> {
          submissions.incrementAndGet();
          return Map.of("name", name, "status", "pending");
        };
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, LongRunningWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(reports);
      test.environment.start();
      LongRunningWorkflow workflow = test.workflow(LongRunningWorkflow.class);
      WorkflowClient.start(workflow::prepare, "Prepare a sales report.");
      AdkTestEnvironment.await(() -> !workflow.pendingCall().isEmpty());
      String callId = workflow.pendingCall();
      test.replay(test.history(workflow), LongRunningWorkflowImpl.class);
      workflow.complete("wrong-call", "ignore this");
      assertEquals(callId, workflow.pendingCall());
      workflow.complete(callId, "Revenue grew 10%.");
      assertEquals(
          "Report complete: Revenue grew 10%.",
          WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(1, submissions.get());
      assertEquals(
          List.of(
              "googleadk.InvokeModel",
              "StartReport",
              "googleadk.InvokeModel",
              "googleadk.InvokeModel"),
          AdkTestEnvironment.activities(test.history(workflow)));
      test.replay(test.history(workflow), LongRunningWorkflowImpl.class);
      assertEquals(1, submissions.get());
    }
  }

  @Test
  void cancellationEndsTheDurableWaitWithoutAnotherModelCall() throws Exception {
    ScriptedModel model = model();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, LongRunningWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(
          (ReportActivities) name -> Map.of("name", name, "status", "pending"));
      test.environment.start();
      LongRunningWorkflow workflow = test.workflow(LongRunningWorkflow.class);
      WorkflowClient.start(workflow::prepare, "Prepare a report.");
      AdkTestEnvironment.await(() -> !workflow.pendingCall().isEmpty());
      WorkflowStub stub = WorkflowStub.fromTyped(workflow);
      stub.cancel();
      WorkflowFailedException failure =
          assertThrows(WorkflowFailedException.class, () -> stub.getResult(String.class));
      assertInstanceOf(CanceledFailure.class, failure.getCause());
      assertEquals(2, model.requests().size());
      test.replay(test.history(workflow), LongRunningWorkflowImpl.class);
    }
  }
}
