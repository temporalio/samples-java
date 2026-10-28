package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.streaming.ModelChunk;
import io.temporal.samples.googleadk.streaming.StreamingWorkflow;
import io.temporal.samples.googleadk.streaming.StreamingWorkflowImpl;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class StreamingWorkflowTest {
  private static ScriptedModel model() {
    return ScriptedModel.streaming(
        AgentSupport.MODEL,
        request ->
            List.of(
                ScriptedModel.partialText("Hel"),
                ScriptedModel.partialText("lo"),
                ScriptedModel.text("Hello")));
  }

  @Test
  void noSubscriberIsRequiredAndTheBufferedChunksReplayInOrder() throws Exception {
    ScriptedModel model = model();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, StreamingWorkflowImpl.class)) {
      test.environment.start();
      StreamingWorkflow workflow = test.workflow(StreamingWorkflow.class);
      assertEquals(
          List.of(
              new ModelChunk("partial", "Hel"),
              new ModelChunk("partial", "lo"),
              new ModelChunk("final", "Hello")),
          workflow.stream("Hello"));
      assertEquals(
          List.of("googleadk.InvokeModel"), AdkTestEnvironment.activities(test.history(workflow)));
      test.replay(test.history(workflow), StreamingWorkflowImpl.class);
    }
  }

  @Test
  void subscriberReceivesEveryChunkAndAcknowledgesCompletion() throws Exception {
    ScriptedModel model = model();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, StreamingWorkflowImpl.class)) {
      test.environment.start();
      StreamingWorkflow workflow = test.workflow(StreamingWorkflow.class);
      var execution = WorkflowClient.start(workflow::stream, "Hello");
      List<ModelChunk> received = new ArrayList<>();
      try (WorkflowStreamClient stream =
              WorkflowStreamClient.newInstance(test.client, execution.getWorkflowId());
          WorkflowStreamSubscription subscription = stream.topic("model").subscribe(0)) {
        for (WorkflowStreamItem item : subscription) {
          ModelChunk chunk =
              test.client
                  .getOptions()
                  .getDataConverter()
                  .fromPayload(item.getPayload(), ModelChunk.class, ModelChunk.class);
          received.add(chunk);
          if (chunk.kind().equals("complete")) {
            workflow.acknowledge();
            break;
          }
        }
      }
      assertEquals(
          List.of(
              new ModelChunk("partial", "Hel"),
              new ModelChunk("partial", "lo"),
              new ModelChunk("final", "Hello"),
              new ModelChunk("complete", "Hello")),
          received);
      assertEquals(3, WorkflowStub.fromTyped(workflow).getResult(List.class).size());
      test.replay(test.history(workflow), StreamingWorkflowImpl.class);
    }
  }
}
