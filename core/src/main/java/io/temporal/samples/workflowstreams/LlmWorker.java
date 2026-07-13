package io.temporal.samples.workflowstreams;

import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Worker for the LLM-streaming scenario. Runs separately from {@link StreamsWorker} so the OpenAI
 * dependency and the {@code OPENAI_API_KEY} requirement stay isolated to this one scenario.
 * Different task queue too — the other five scenarios won't route work to this worker.
 *
 * <p>Kill this worker mid-stream while {@link Llm} is running (and restart it) to trigger a retry:
 * Temporal restarts the activity, the activity publishes a RetryEvent on its second attempt, and
 * the consumer resets its rendered output.
 */
public class LlmWorker {

  public static void main(String[] args) {
    WorkflowClient client = Shared.newWorkflowClient();
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(Shared.LLM_TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(LlmWorkflowImpl.class);
    worker.registerActivitiesImplementations(new LlmActivitiesImpl());

    factory.start();
    System.out.println("LLM worker started for task queue: " + Shared.LLM_TASK_QUEUE);
  }
}
