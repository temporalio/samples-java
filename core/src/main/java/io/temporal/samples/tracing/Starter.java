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

package io.temporal.samples.tracing;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.samples.tracing.workflow.TracingWorkflow;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Starter {
  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  public static final String TASK_QUEUE_NAME = "tracingTaskQueue";

  public static void main(String[] args) {
    String type = "OpenTelemetry";
    if (args.length == 1) {
      type = args[0];
    }

    // Set the OpenTracing client interceptor
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder()
            .setInterceptors(new OpenTracingClientInterceptor(JaegerUtils.getJaegerOptions(type)))
            .build();
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("tracingWorkflow")
            .setTaskQueue(TASK_QUEUE_NAME)
            .build();

    // Create typed workflow stub
    TracingWorkflow workflow = client.newWorkflowStub(TracingWorkflow.class, workflowOptions);

    // Convert to untyped and start it with signalWithStart
    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    untyped.signalWithStart("setLanguage", new Object[] {"Spanish"}, new Object[] {"John"});

    String greeting = untyped.getResult(String.class);

    System.out.println("Greeting: " + greeting);

    System.exit(0);
  }
}
