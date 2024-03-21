/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.temporal.samples.hello

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.failure.ApplicationFailure
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import java.time.Duration

/**
 * Sample Temporal workflow that demonstrates how to use workflow await methods to wait up to a
 * specified timeout for a condition updated from a signal handler.
 */
object HelloAwait {
  // Define the task queue name
  const val TASK_QUEUE: String = "HelloAwaitTaskQueue"

  // Define the workflow unique id
  const val WORKFLOW_ID: String = "HelloAwaitWorkflow"

  /**
   * With the Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  @Throws(
    Exception::class) @JvmStatic fun main(args: Array<String>) {
    // Get a Workflow service stub.

    val service = WorkflowServiceStubs.newLocalServiceStubs()

    /*
     * Get a Workflow service client which can be used to start, Await, and Query Workflow Executions.
     */
    val client = WorkflowClient.newInstance(service)

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    val factory = WorkerFactory.newInstance(client)

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    val worker = factory.newWorker(TASK_QUEUE)

    /*
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl::class.java)

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start()

    // Create the workflow options
    val workflowOptions =
      WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build()

    // Create the workflow client stub. It is used to start the workflow execution.
    val workflow = client.newWorkflowStub(GreetingWorkflow::class.java, workflowOptions)

    // Start workflow asynchronously and call its getGreeting workflow method
    WorkflowClient.start<String?> { workflow.getGreeting(HelloAwait.GreetingPayload("foobar")) }

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // Send WaitForName signal.
    workflow.waitForName("World")

    /*
     * Here we create a new untyped workflow stub using the same workflow id.
     * The untyped stub is a convenient way to wait for a workflow result.
     */
    val workflowById = client.newUntypedWorkflowStub(WORKFLOW_ID)

    val greeting = workflowById.getResult(String::class.java)

    println(greeting)
    System.exit(0)
  }

  data class GreetingPayload(
    var idempotenceToken: String
  )

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   *
   * Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see WorkflowInterface
   *
   * @see WorkflowMethod
   */
  @WorkflowInterface
  interface GreetingWorkflow {
    @WorkflowMethod fun getGreeting(greetingPayload: GreetingPayload): String?

    // Define the workflow waitForName signal method. This method is executed when the workflow
    // receives a "WaitForName" signal.
    @SignalMethod fun waitForName(name: String?)
  }

  // Define the workflow implementation which implements the getGreetings workflow method.
  class GreetingWorkflowImpl : GreetingWorkflow {
    private var name: String? = null

    override fun getGreeting(greetingPayload: GreetingPayload): String? {
        val ok = Workflow.await(Duration.ofSeconds(10)
        ) { name != null }
        if (ok) {
          return "Hello $name!"
        } else {
          // To fail workflow use ApplicationFailure. Any other exception would cause workflow to
          // stall, not to fail.
          throw ApplicationFailure.newFailure(
            "WaitForName signal is not received within 10 seconds.", "signal-timeout")
        }
      }

    override fun waitForName(name: String?) {
      this.name = name
    }
  }
}
