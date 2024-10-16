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

package io.temporal.samples.nexus.caller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.handler.NexusServiceImpl;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class CallerWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          // If a Nexus service is registered as part of the test, the TestWorkflowRule will ,by
          // default, automatically create a Nexus service endpoint and workflows registered as part
          // of the TestWorkflowRule will automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(new NexusServiceImpl())
          .setWorkflowTypes(HelloCallerWorkflowImpl.class)
          // Disable automatic worker startup as we are going to register some workflows manually
          // per test
          .setDoNotStart(true)
          .build();

  @Test
  public void testHelloWorkflow() {
    testWorkflowRule
        .getWorker()
        // Workflows started by a Nexus service can be mocked just like any other workflow
        .registerWorkflowImplementationFactory(
            HelloHandlerWorkflow.class,
            () -> {
              HelloHandlerWorkflow wf = mock(HelloHandlerWorkflow.class);
              when(wf.hello(any())).thenReturn(new NexusService.HelloOutput("Hello World 👋"));
              return wf;
            });
    testWorkflowRule.getTestEnvironment().start();

    HelloCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.hello("World", NexusService.Language.EN);
    assertEquals("Hello World 👋", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testEchoWorkflow() {
    // If Workflows are registered later than the endpoint can be set manually
    // either by setting the endpoint in the NexusServiceOptions in the Workflow implementation or
    // by setting the NexusServiceOptions on the WorkflowImplementationOptions when registering the
    // Workflow.
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                .setNexusServiceOptions(
                    Collections.singletonMap(
                        "NexusService",
                        NexusServiceOptions.newBuilder()
                            .setEndpoint(testWorkflowRule.getNexusEndpoint().getSpec().getName())
                            .build()))
                .build(),
            EchoCallerWorkflowImpl.class);
    testWorkflowRule.getTestEnvironment().start();

    EchoCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                EchoCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.echo("Hello");
    assertEquals("Hello", greeting);
    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
