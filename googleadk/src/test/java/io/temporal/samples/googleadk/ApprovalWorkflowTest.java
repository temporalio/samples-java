package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.failure.CanceledFailure;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.humanintheloop.ApprovalWorkflow;
import io.temporal.samples.googleadk.humanintheloop.ApprovalWorkflowImpl;
import io.temporal.samples.googleadk.humanintheloop.OrderActivities;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(30)
class ApprovalWorkflowTest {
  private static ScriptedModel model() {
    return ScriptedModel.responding(
        AgentSupport.MODEL,
        request ->
            AdkTestEnvironment.functionResponse(request, "shipOrder")
                .map(
                    response -> {
                      if (response.containsKey("error")) {
                        return ScriptedModel.text(
                            String.valueOf(response.get("error")).contains("rejected")
                                ? "Order rejected."
                                : "Please approve the shipment.");
                      }
                      assertEquals("demo-order", response.get("orderId"));
                      return ScriptedModel.text("Order shipped.");
                    })
                .orElseGet(
                    () ->
                        ScriptedModel.functionCall("shipOrder", Map.of("orderId", "demo-order"))));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void orderShipsOnlyAfterConfirmation(boolean confirmed) throws Exception {
    ScriptedModel model = model();
    AtomicInteger shipments = new AtomicInteger();
    OrderActivities orders =
        order -> {
          shipments.incrementAndGet();
          return Map.of("orderId", order, "status", "shipped");
        };
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ApprovalWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(orders);
      test.environment.start();
      ApprovalWorkflow workflow = test.workflow(ApprovalWorkflow.class);
      WorkflowClient.start(workflow::fulfill, "Ship demo-order");
      AdkTestEnvironment.await(() -> !workflow.pendingConfirmations().isEmpty());

      assertEquals("shipOrder", workflow.pendingConfirmations().get(0).toolName());
      assertEquals(
          "demo-order",
          workflow
              .pendingConfirmations()
              .get(0)
              .originalFunctionCall()
              .args()
              .orElseThrow()
              .get("orderId"));
      assertEquals(0, shipments.get());
      assertFalse(AdkTestEnvironment.activities(test.history(workflow)).contains("ShipOrder"));

      workflow.decide(confirmed);
      assertEquals(
          confirmed ? "Order shipped." : "Order rejected.",
          WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(confirmed ? 1 : 0, shipments.get());
      WorkflowExecutionHistory history = test.history(workflow);
      assertEquals(confirmed, AdkTestEnvironment.activities(history).contains("ShipOrder"));
      test.replay(history, ApprovalWorkflowImpl.class);
      assertEquals(confirmed ? 1 : 0, shipments.get());
    }
  }

  @Test
  void aPrematureDecisionCannotApproveAFutureToolCall() throws Exception {
    ScriptedModel model = model();
    AtomicInteger shipments = new AtomicInteger();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ApprovalWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(
          (OrderActivities)
              order -> {
                shipments.incrementAndGet();
                return Map.of("orderId", order, "status", "shipped");
              });
      test.environment.start();
      ApprovalWorkflow workflow = test.workflow(ApprovalWorkflow.class);
      WorkflowStub stub = WorkflowStub.fromTyped(workflow);
      stub.signalWithStart("decide", new Object[] {true}, new Object[] {"Ship demo-order"});
      AdkTestEnvironment.await(() -> !workflow.pendingConfirmations().isEmpty());
      workflow.decide(false);
      assertEquals("Order rejected.", stub.getResult(String.class));
      assertEquals(0, shipments.get());
      test.replay(test.history(workflow), ApprovalWorkflowImpl.class);
    }
  }

  @Test
  void cancellationWhileAwaitingApprovalNeverShipsTheOrder() throws Exception {
    ScriptedModel model = model();
    AtomicInteger shipments = new AtomicInteger();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ApprovalWorkflowImpl.class)) {
      test.worker.registerActivitiesImplementations(
          (OrderActivities)
              order -> {
                shipments.incrementAndGet();
                return Map.of("orderId", order, "status", "shipped");
              });
      test.environment.start();
      ApprovalWorkflow workflow = test.workflow(ApprovalWorkflow.class);
      WorkflowClient.start(workflow::fulfill, "Ship demo-order");
      AdkTestEnvironment.await(() -> !workflow.pendingConfirmations().isEmpty());
      WorkflowStub stub = WorkflowStub.fromTyped(workflow);
      stub.cancel();
      WorkflowFailedException failure =
          assertThrows(WorkflowFailedException.class, () -> stub.getResult(String.class));
      assertInstanceOf(CanceledFailure.class, failure.getCause());
      assertEquals(0, shipments.get());
      assertFalse(AdkTestEnvironment.activities(test.history(workflow)).contains("ShipOrder"));
      test.replay(test.history(workflow), ApprovalWorkflowImpl.class);
    }
  }
}
