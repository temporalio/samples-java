package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.adk.models.LlmRequest;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.client.WorkflowUpdateHandle;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.failure.ApplicationFailure;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.chat.ChatInput;
import io.temporal.samples.googleadk.chat.ChatWorkflow;
import io.temporal.samples.googleadk.chat.ChatWorkflowImpl;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(30)
class ChatWorkflowTest {
  private static List<String> messages(LlmRequest request) {
    return request.contents().stream()
        .filter(content -> content.role().orElse("").equals("user"))
        .flatMap(content -> content.parts().orElse(List.of()).stream())
        .flatMap(part -> part.text().stream())
        .toList();
  }

  private static WorkflowUpdateHandle<String> message(ChatWorkflow workflow, String text) {
    return WorkflowStub.fromTyped(workflow)
        .startUpdate("message", WorkflowUpdateStage.ACCEPTED, String.class, text);
  }

  private static void blockModel(CountDownLatch entered, CountDownLatch release) {
    entered.countDown();
    try {
      if (!release.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("The test did not release the model call.");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static boolean continuedAsNew(WorkflowExecutionHistory history) {
    return history.getEvents().stream()
        .anyMatch(
            event ->
                event.getEventType() == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_CONTINUED_AS_NEW);
  }

  @Test
  void updatesReturnTheirOwnRepliesAndReplay() throws Exception {
    // @@@SNIPSTART java-google-adk-chat-test
    ScriptedModel model =
        ScriptedModel.of(
            AgentSupport.MODEL,
            ScriptedModel.text("First reply"),
            ScriptedModel.text("Last reply"));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      WorkflowClient.start(workflow::chat, ChatInput.empty());

      assertEquals("First reply", workflow.message("one"));
      assertEquals("Last reply", workflow.message("two"));
      workflow.finish();
      assertEquals("Last reply", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(2, model.requests().size());
      test.replay(test.history(workflow), ChatWorkflowImpl.class);
    }
    // @@@SNIPEND
  }

  @Test
  void concurrentUpdatesFinishBeforeContinueAsNewAndKeepTheConversation() throws Exception {
    CountDownLatch thirdEntered = new CountDownLatch(1);
    CountDownLatch releaseThird = new CountDownLatch(1);
    CountDownLatch fourthEntered = new CountDownLatch(1);
    CountDownLatch releaseFourth = new CountDownLatch(1);
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request -> {
              List<String> turns = messages(request);
              if (turns.size() == 3) {
                blockModel(thirdEntered, releaseThird);
              } else if (turns.size() == 4) {
                blockModel(fourthEntered, releaseFourth);
              }
              return ScriptedModel.text(String.join(", ", turns));
            });
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow starter = test.workflow(ChatWorkflow.class);
      WorkflowExecution firstRun = WorkflowClient.start(starter::chat, ChatInput.empty());
      ChatWorkflow workflow =
          test.client.newWorkflowStub(ChatWorkflow.class, firstRun.getWorkflowId());
      assertEquals("one", workflow.message("one"));
      assertEquals("one, two", workflow.message("two"));

      WorkflowUpdateHandle<String> third = message(workflow, "three");
      assertTrue(thirdEntered.await(10, TimeUnit.SECONDS));
      WorkflowUpdateHandle<String> fourth = message(workflow, "four");
      assertEquals(3, AdkTestEnvironment.activities(test.history(workflow)).size());
      releaseThird.countDown();
      assertEquals("one, two, three", third.getResult());
      assertTrue(fourthEntered.await(10, TimeUnit.SECONDS));
      assertFalse(
          continuedAsNew(test.client.fetchHistory(firstRun.getWorkflowId(), firstRun.getRunId())));
      releaseFourth.countDown();
      assertEquals("one, two, three, four", fourth.getResult());
      AdkTestEnvironment.await(
          () ->
              continuedAsNew(
                  test.client.fetchHistory(firstRun.getWorkflowId(), firstRun.getRunId())));

      assertEquals("one, two, three, four", workflow.lastReply());
      assertEquals("one, two, three, four, five", workflow.message("five"));
      workflow.finish();
      assertEquals(
          "one, two, three, four, five", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(5, model.requests().size());
      WorkflowExecutionHistory firstHistory =
          test.client.fetchHistory(firstRun.getWorkflowId(), firstRun.getRunId());
      WorkflowExecutionHistory lastHistory = test.history(workflow);
      assertEquals(
          firstRun.getRunId(),
          lastHistory
              .getEvents()
              .get(0)
              .getWorkflowExecutionStartedEventAttributes()
              .getContinuedExecutionRunId());
      assertEquals(4, AdkTestEnvironment.activities(firstHistory).size());
      assertEquals(1, AdkTestEnvironment.activities(lastHistory).size());
      test.replay(firstHistory, ChatWorkflowImpl.class);
      test.replay(lastHistory, ChatWorkflowImpl.class);
    } finally {
      releaseThird.countDown();
      releaseFourth.countDown();
    }
  }

  @Test
  void finishDrainsAcceptedUpdatesAndRejectsNewMessages() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request -> {
              List<String> turns = messages(request);
              if (turns.size() == 1) {
                blockModel(entered, release);
              }
              return ScriptedModel.text(String.join(", ", turns));
            });
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      WorkflowClient.start(workflow::chat, ChatInput.empty());
      WorkflowUpdateHandle<String> first = message(workflow, "one");
      assertTrue(entered.await(10, TimeUnit.SECONDS));
      WorkflowUpdateHandle<String> second = message(workflow, "two");
      assertEquals(1, AdkTestEnvironment.activities(test.history(workflow)).size());

      workflow.finish();
      assertEquals("", workflow.lastReply());
      WorkflowUpdateException failure =
          assertThrows(WorkflowUpdateException.class, () -> workflow.message("late"));
      assertEquals(
          IllegalStateException.class.getName(),
          assertInstanceOf(ApplicationFailure.class, failure.getCause()).getType());
      release.countDown();
      assertEquals("one", first.getResult());
      assertEquals("one, two", second.getResult());
      assertEquals("one, two", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(2, model.requests().size());
      test.replay(test.history(workflow), ChatWorkflowImpl.class);
    } finally {
      release.countDown();
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void invalidMessagesAreRejectedWithoutCallingTheModel(String text) throws Exception {
    ScriptedModel model = ScriptedModel.of(AgentSupport.MODEL, ScriptedModel.text("ok"));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      WorkflowClient.start(workflow::chat, ChatInput.empty());
      WorkflowUpdateException failure =
          assertThrows(WorkflowUpdateException.class, () -> workflow.message(text));
      assertEquals(
          IllegalArgumentException.class.getName(),
          assertInstanceOf(ApplicationFailure.class, failure.getCause()).getType());
      assertTrue(model.requests().isEmpty());
      assertEquals("ok", workflow.message("valid"));
      workflow.finish();
      assertEquals("ok", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(1, model.requests().size());
      test.replay(test.history(workflow), ChatWorkflowImpl.class);
    }
  }

  @Test
  void finishBeforeTheFirstTurnCompletesWithoutCallingTheModel() throws Exception {
    ScriptedModel model = ScriptedModel.of(AgentSupport.MODEL, ScriptedModel.text("unused"));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      WorkflowClient.start(workflow::chat, ChatInput.empty());
      workflow.finish();
      assertEquals("", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertTrue(model.requests().isEmpty());
      assertTrue(AdkTestEnvironment.activities(test.history(workflow)).isEmpty());
      test.replay(test.history(workflow), ChatWorkflowImpl.class);
    }
  }

  @Test
  void failedUpdatesReleaseTheTurnLockAndTheConversationCanContinue() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request -> {
              if (attempts.incrementAndGet() == 1) {
                throw ApplicationFailure.newNonRetryableFailure("Model unavailable", "ModelDenied");
              }
              return ScriptedModel.text("Recovered");
            });
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      WorkflowClient.start(workflow::chat, ChatInput.empty());
      assertThrows(WorkflowUpdateException.class, () -> workflow.message("one"));
      assertEquals("", workflow.lastReply());
      assertEquals("Recovered", workflow.message("two"));
      workflow.finish();
      assertEquals("Recovered", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(2, attempts.get());
      assertEquals(2, model.requests().size());
      test.replay(test.history(workflow), ChatWorkflowImpl.class);
    }
  }
}
