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

import static org.hamcrest.MatcherAssert.assertThat;

import io.temporal.testing.WorkflowReplayer;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for replay {@link HelloActivity.GreetingWorkflowImpl}. Doesn't use an external Temporal
 * service.
 */
public class HelloActivityReplayTest {

  @Test
  public void replayWorkflowExecutionFromResource() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "hello_activity_replay.json", HelloActivity.GreetingWorkflowImpl.class);
  }

  @Test
  public void replayWorkflowExecutionFromResourceNonDeterministic() {

    // hello_activity_replay_non_deterministic.json Event History is the result of executing a
    // different
    // workflow implementation than HelloActivity.GreetingWorkflowImpl.class therefore we expect an
    // exception during the replay
    try {
      WorkflowReplayer.replayWorkflowExecutionFromResource(
          "hello_activity_replay_non_deterministic.json", HelloActivity.GreetingWorkflowImpl.class);

      Assert.fail("Should have thrown an Exception");
    } catch (Exception e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.containsString("error=io.temporal.worker.NonDeterministicException"));
    }
  }
}
