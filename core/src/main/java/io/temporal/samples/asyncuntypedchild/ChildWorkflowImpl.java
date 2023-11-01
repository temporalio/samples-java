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

package io.temporal.samples.asyncuntypedchild;

import io.temporal.workflow.Workflow;

/**
 * Define the parent workflow implementation. It implements the getGreeting workflow method
 *
 * <p>Note that a workflow implementation must always be public for the Temporal library to be able
 * to create its instances.
 */
public class ChildWorkflowImpl implements ChildWorkflow {

  @Override
  public String composeGreeting(String greeting, String name) {

    // Sleep for 2 seconds to ensure the child completes after the parent.
    Workflow.sleep(2000);

    return greeting + " " + name + "!";
  }
}
