package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamState;
import java.io.IOException;

/**
 * Constants and shared types for the workflow streams sample. A workflow stream is a durable
 * publish/subscribe log hosted inside a Temporal workflow: external code publishes to named topics
 * via signals, subscribers long-poll for new items via updates, and a query exposes the current
 * offset. See the experimental {@code io.temporal:temporal-workflowstreams} module.
 */
public final class Shared {

  /**
   * Task queues. The LLM scenario runs on its own queue so its OpenAI dependency and API key
   * requirement stay isolated from the other workflows.
   */
  public static final String TASK_QUEUE = "workflow-streams";

  public static final String LLM_TASK_QUEUE = "workflow-streams-llm";

  /** Topic names used across the scenarios. */
  public static final String TOPIC_STATUS = "status";

  public static final String TOPIC_PROGRESS = "progress";
  public static final String TOPIC_NEWS = "news";
  public static final String TOPIC_TICK = "tick";
  public static final String TOPIC_DELTA = "delta";
  public static final String TOPIC_COMPLETE = "complete";
  public static final String TOPIC_RETRY = "retry";

  /** Creates a WorkflowClient from environment configuration. */
  public static WorkflowClient newWorkflowClient() {
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    return WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }

  /** Decodes a subscribed item's payload with the default data converter. */
  public static <T> T decode(WorkflowStreamItem item, Class<T> type) {
    return DefaultDataConverter.STANDARD_INSTANCE.fromPayload(item.getPayload(), type, type);
  }

  // Each workflow input carries an optional WorkflowStreamState so the stream can survive
  // continue-as-new: thread the prior run's state back in and pass it to
  // WorkflowStream.newInstance. It is null on a fresh start.

  /** Input to OrderWorkflow (scenarios 1 and 2). */
  public static class OrderInput {
    public String orderId;
    public WorkflowStreamState streamState;

    public OrderInput() {}

    public OrderInput(String orderId) {
      this.orderId = orderId;
    }
  }

  /** Input to PipelineWorkflow (scenario 3). */
  public static class PipelineInput {
    public String pipelineId;
    public WorkflowStreamState streamState;

    public PipelineInput() {}

    public PipelineInput(String pipelineId) {
      this.pipelineId = pipelineId;
    }
  }

  /** Input to HubWorkflow (scenario 4). */
  public static class HubInput {
    public String hubId;
    public WorkflowStreamState streamState;

    public HubInput() {}

    public HubInput(String hubId) {
      this.hubId = hubId;
    }
  }

  /**
   * Input to TickerWorkflow (scenario 5). Zero-valued fields fall back to the defaults applied in
   * the workflow.
   */
  public static class TickerInput {
    public int count;
    public int keepLast;
    public int truncateEvery;
    public long intervalMs;
    public WorkflowStreamState streamState;

    public TickerInput() {}

    public TickerInput(int count, int keepLast, int truncateEvery) {
      this.count = count;
      this.keepLast = keepLast;
      this.truncateEvery = truncateEvery;
    }
  }

  /** Input to LlmWorkflow (scenario 6). */
  public static class LlmInput {
    public String prompt;
    public String model;
    public WorkflowStreamState streamState;

    public LlmInput() {}

    public LlmInput(String prompt, String model) {
      this.prompt = prompt;
      this.model = model;
    }
  }

  // Event types published to the stream. They are JSON-encoded by the default data converter on
  // the way in and decoded by subscribers on the way out.

  /** Reports an order's lifecycle stage on TOPIC_STATUS. */
  public static class StatusEvent {
    public String kind;
    public String orderId;

    public StatusEvent() {}

    public StatusEvent(String kind, String orderId) {
      this.kind = kind;
      this.orderId = orderId;
    }
  }

  /** Reports fine-grained progress on TOPIC_PROGRESS. */
  public static class ProgressEvent {
    public String message;

    public ProgressEvent() {}

    public ProgressEvent(String message) {
      this.message = message;
    }
  }

  /** Reports a pipeline stage on TOPIC_STATUS. */
  public static class StageEvent {
    public String stage;

    public StageEvent() {}

    public StageEvent(String stage) {
      this.stage = stage;
    }
  }

  /** Published by an external publisher on TOPIC_NEWS. */
  public static class NewsEvent {
    public String headline;

    public NewsEvent() {}

    public NewsEvent(String headline) {
      this.headline = headline;
    }
  }

  /** Published by the ticker on TOPIC_TICK. */
  public static class TickEvent {
    public int n;

    public TickEvent() {}

    public TickEvent(int n) {
      this.n = n;
    }
  }

  /** A single streamed token chunk on TOPIC_DELTA. */
  public static class TextDelta {
    public String text;

    public TextDelta() {}

    public TextDelta(String text) {
      this.text = text;
    }
  }

  /** The final accumulated completion on TOPIC_COMPLETE. */
  public static class TextComplete {
    public String fullText;

    public TextComplete() {}

    public TextComplete(String fullText) {
      this.fullText = fullText;
    }
  }

  /**
   * Signals that the streaming activity is on a retry attempt, so subscribers can reset any
   * partially rendered output.
   */
  public static class RetryEvent {
    public int attempt;

    public RetryEvent() {}

    public RetryEvent(int attempt) {
      this.attempt = attempt;
    }
  }

  private Shared() {}
}
