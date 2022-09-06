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

package io.temporal.samples.hello;

import static org.mockito.Mockito.*;

import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit test for {@link HelloChild}. Doesn't use an external Temporal service. */
public class HelloChildJUnit5Test {

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(HelloChild.GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testMockedChild(
      TestWorkflowEnvironment testEnv, Worker worker, HelloChild.GreetingWorkflow workflow) {

    // As new mock is created on each workflow task the only last one is useful to verify calls.
    AtomicReference<HelloChild.GreetingChild> lastChildMock = new AtomicReference<>();
    // Factory is called to create a new workflow object on each workflow task.
    worker.addWorkflowImplementationFactory(
        HelloChild.GreetingChild.class,
        () -> {
          HelloChild.GreetingChild child = mock(HelloChild.GreetingChild.class);
          when(child.composeGreeting("Hello", "World")).thenReturn("Bye World!");
          lastChildMock.set(child);
          return child;
        });

    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    Assert.assertEquals("Bye World!", greeting);
    HelloChild.GreetingChild mock = lastChildMock.get();
    verify(mock).composeGreeting(eq("Hello"), eq("World"));

    testEnv.shutdown();
  }
}
