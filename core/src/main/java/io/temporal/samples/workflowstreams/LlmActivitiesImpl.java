package io.temporal.samples.workflowstreams;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.temporal.activity.Activity;
import io.temporal.samples.workflowstreams.Shared.LlmInput;
import io.temporal.samples.workflowstreams.Shared.RetryEvent;
import io.temporal.samples.workflowstreams.Shared.TextComplete;
import io.temporal.samples.workflowstreams.Shared.TextDelta;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamClientOptions;
import java.time.Duration;

/**
 * Calls OpenAI with streaming enabled and republishes each token delta to the workflow stream. The
 * accumulated text is published on the complete topic and returned as the activity result. Because
 * the activity owns the non-deterministic OpenAI call, the workflow stays deterministic.
 *
 * <p>Retries are disabled on the OpenAI client so transient failures surface as Temporal activity
 * retries instead. On a retry (attempt &gt; 1) it publishes a RetryEvent so subscribers can reset
 * partially rendered output.
 */
public class LlmActivitiesImpl implements LlmActivities {

  static final String DEFAULT_MODEL = "gpt-4o-mini";

  @Override
  public String streamCompletion(LlmInput input) {
    WorkflowStreamClientOptions options =
        WorkflowStreamClientOptions.newBuilder().setBatchInterval(Duration.ofMillis(200)).build();
    try (WorkflowStreamClient streamClient = WorkflowStreamClient.fromActivity(options)) {
      TopicHandle deltas = streamClient.topic(Shared.TOPIC_DELTA);
      TopicHandle complete = streamClient.topic(Shared.TOPIC_COMPLETE);
      TopicHandle retry = streamClient.topic(Shared.TOPIC_RETRY);

      int attempt = Activity.getExecutionContext().getInfo().getAttempt();
      if (attempt > 1) {
        retry.publish(new RetryEvent(attempt), /* forceFlush */ true);
      }

      String model = input.model != null && !input.model.isEmpty() ? input.model : DEFAULT_MODEL;

      // Reads OPENAI_API_KEY from the environment.
      OpenAIClient openai = OpenAIOkHttpClient.builder().fromEnv().maxRetries(0).build();
      ChatCompletionCreateParams params =
          ChatCompletionCreateParams.builder().model(model).addUserMessage(input.prompt).build();

      StringBuilder full = new StringBuilder();
      try (StreamResponse<ChatCompletionChunk> stream =
          openai.chat().completions().createStreaming(params)) {
        stream.stream()
            .forEach(
                chunk ->
                    chunk.choices().stream()
                        .findFirst()
                        .flatMap(choice -> choice.delta().content())
                        .filter(text -> !text.isEmpty())
                        .ifPresent(
                            text -> {
                              deltas.publish(new TextDelta(text));
                              full.append(text);
                            }));
      }

      String fullText = full.toString();
      complete.publish(new TextComplete(fullText), /* forceFlush */ true);
      return fullText;
    }
  }
}
