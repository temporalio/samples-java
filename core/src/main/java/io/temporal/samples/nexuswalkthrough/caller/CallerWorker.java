package io.temporal.samples.nexuswalkthrough.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexuswalkthrough.options.ClientOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;

/**
 * WALKTHROUGH STEP 6 - The caller Worker.
 *
 * <p>This Worker runs in the caller Namespace and knows nothing about the handler beyond the
 * Endpoint name bound here. That binding is the only place the caller side names the Endpoint; the
 * caller Workflow itself refers to the Service by its contract alone.
 */
public class CallerWorker {

  public static final String DEFAULT_TASK_QUEUE_NAME = "approval-caller-task-queue";
  public static final String DEFAULT_ENDPOINT_NAME = "approval-endpoint";

  /**
   * The Service name as it appears on the wire. The generated interface is annotated
   * {@code @Service(name = "...")} with the contract's fully qualified name, so that - not the Java
   * interface's simple name - is the key the Endpoint binding is registered under.
   */
  public static final String SERVICE_NAME = "temporal.samples.approval.v1.ApprovalService";

  // @@@SNIPSTART samples-java-nexus-walkthrough-caller-worker
  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);

    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                Collections.singletonMap(
                    SERVICE_NAME,
                    NexusServiceOptions.newBuilder().setEndpoint(DEFAULT_ENDPOINT_NAME).build()))
            .build(),
        ApprovalCallerWorkflowImpl.class);

    factory.start();
  }
  // @@@SNIPEND
}
