package io.temporal.samples.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/** Sample Temporal workflow that demonstrates workflow activity retries. */
public class HelloActivityRetry {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloActivityWithRetriesTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloActivityWithRetriesWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {

    /** Define your activity method which can be called during workflow execution */
    String composeGreeting(String greeting, String name);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     *
     * <p>In the {@link ActivityOptions} definition the "setInitialInterval" option sets the
     * interval of the first retry. It is set to 1 second. The "setDoNotRetry" option is a list of
     * application failures for which retries should not be performed.
     *
     * <p>By default the maximum number of retry attempts is set to "unlimited" however you can
     * change it by adding the "setMaximumAttempts" option to the retry options.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setDoNotRetry(IllegalArgumentException.class.getName())
                        .build())
                .build());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after activity is completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  /**
   * Implementation of our workflow activity interface. It overwrites our defined composeGreeting
   * activity method.
   */
  static class GreetingActivitiesImpl implements GreetingActivities {
    private int callCount;
    private long lastInvocationTime;

    /**
     * Our activity implementation simulates a failure 3 times. Given our previously set
     * RetryOptions, our workflow is going to retry our activity execution.
     */
    @Override
    public synchronized String composeGreeting(String greeting, String name) {
      if (lastInvocationTime != 0) {
        long timeSinceLastInvocation = System.currentTimeMillis() - lastInvocationTime;
        System.out.print(timeSinceLastInvocation + " milliseconds since last invocation. ");
      }
      lastInvocationTime = System.currentTimeMillis();
      if (++callCount < 4) {
        System.out.println("composeGreeting activity is going to fail");

        /*
         * We throw IllegalStateException here. It is not in the list of "do not retry" exceptions
         * set in our RetryOptions, so a workflow retry is going to be issued
         */
        throw new IllegalStateException("not yet");
      }

      // after 3 unsuccessful retries we finally can complete our activity execution
      System.out.println("composeGreeting activity is going to complete");
      return greeting + " " + name + "!";
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Set our workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TASK_QUEUE).build();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     *
     * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */
    String greeting = workflow.getGreeting("World");

    // Display workflow execution results
    System.out.println(greeting);
    System.exit(0);
  }
}
