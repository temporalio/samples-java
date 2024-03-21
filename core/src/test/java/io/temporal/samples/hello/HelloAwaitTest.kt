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

import com.squareup.payrollrunner.HelloAwait
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowException
import io.temporal.client.WorkflowOptions
import io.temporal.failure.ApplicationFailure
import com.squareup.payrollrunner.HelloAwait.GreetingWorkflow
import io.temporal.testing.TestWorkflowRule
import java.time.Duration
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/** Unit test for [HelloAwait]. Doesn't use an external Temporal service.  */
class HelloAwaitTest {
  private val WORKFLOW_ID = "WORKFLOW1"

  @get:Rule
  var testWorkflowRule: TestWorkflowRule = TestWorkflowRule.newBuilder().setWorkflowTypes(
    HelloAwait.GreetingWorkflowImpl::class.java).build()

  @Test fun testAwaitSignal() {
    // Get a workflow stub using the same task queue the worker uses.
    val workflowOptions =
      WorkflowOptions.newBuilder()
        .setTaskQueue(testWorkflowRule.taskQueue)
        .setWorkflowId(WORKFLOW_ID)
        .build()

    val workflow: GreetingWorkflow =
      testWorkflowRule
        .workflowClient
        .newWorkflowStub(GreetingWorkflow::class.java, workflowOptions)

    // Start workflow asynchronously to not use another thread to await.
    WorkflowClient.start(workflow::getGreeting, HelloAwait.GreetingPayload("foobar"))
    workflow.waitForName("World")

    // So we can send a await to it using workflow stub immediately.
    // But just to demonstrate the unit testing of a long running workflow adding a long sleep here.
    //    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(30));
    val workflowById =
      testWorkflowRule.workflowClient.newUntypedWorkflowStub(WORKFLOW_ID)

    val greeting = workflowById.getResult(String::class.java)
    Assert.assertEquals("Hello World!", greeting)
  }

  @Test fun testAwaitTimeout() {
    // Get a workflow stub using the same task queue the worker uses.
    val workflowOptions =
      WorkflowOptions.newBuilder()
        .setTaskQueue(testWorkflowRule.taskQueue)
        .setWorkflowId(WORKFLOW_ID)
        .build()

    val workflow: GreetingWorkflow =
      testWorkflowRule
        .workflowClient
        .newWorkflowStub(GreetingWorkflow::class.java, workflowOptions)

    // Start workflow asynchronously to not use another thread to wait.
    WorkflowClient.start(workflow::getGreeting, HelloAwait.GreetingPayload("foobar"))

    // Skip time to force Await timeout
    testWorkflowRule.testEnvironment.sleep(Duration.ofSeconds(30))

    val workflowById =
      testWorkflowRule.workflowClient.newUntypedWorkflowStub(WORKFLOW_ID)

    try {
      workflowById.getResult(String::class.java)
      Assert.fail("not reachable")
    } catch (e: WorkflowException) {
      val applicationFailure = e.cause as ApplicationFailure?
      Assert.assertEquals("signal-timeout", applicationFailure!!.type)
    }
  }
}
