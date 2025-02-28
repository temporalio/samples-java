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

package io.temporal.samples.nexuscontextpropagation.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.service.NexusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// To create a service implementation, annotate the class with @ServiceImpl and provide the
// interface that the service implements. The service implementation class should have methods that
// return OperationHandler that correspond to the operations defined in the service interface.
@ServiceImpl(service = NexusService.class)
public class NexusServiceImpl {
  private static final Logger logger = LoggerFactory.getLogger(NexusServiceImpl.class);

  @OperationImpl
  public OperationHandler<NexusService.EchoInput, NexusService.EchoOutput> echo() {
    // OperationHandler.sync is a meant for exposing simple RPC handlers.
    return OperationHandler.sync(
        // The method is for making arbitrary short calls to other services or databases, or
        // perform simple computations such as this one. Users can also access a workflow client by
        // calling
        // Nexus.getOperationContext().getWorkflowClient(ctx) to make arbitrary calls such as
        // signaling, querying, or listing workflows.
        (ctx, details, input) -> {
          if (MDC.get("x-nexus-caller-workflow-id") != null) {
            logger.info(
                "Echo called from a workflow with ID : {}", MDC.get("x-nexus-caller-workflow-id"));
          }
          return new NexusService.EchoOutput(input.getMessage());
        });
  }

  @OperationImpl
  public OperationHandler<NexusService.HelloInput, NexusService.HelloOutput> hello() {
    // Use the WorkflowRunOperation.fromWorkflowMethod constructor, which is the easiest
    // way to expose a workflow as an operation.
    return WorkflowRunOperation.fromWorkflowMethod(
        (ctx, details, input) ->
            Nexus.getOperationContext()
                    .getWorkflowClient()
                    .newWorkflowStub(
                        HelloHandlerWorkflow.class,
                        // Workflow IDs should typically be business meaningful IDs and are used to
                        // dedupe workflow starts.
                        // For this example, we're using the request ID allocated by Temporal when
                        // the
                        // caller workflow schedules
                        // the operation, this ID is guaranteed to be stable across retries of this
                        // operation.
                        //
                        // Task queue defaults to the task queue this operation is handled on.
                        WorkflowOptions.newBuilder().setWorkflowId(details.getRequestId()).build())
                ::hello);
  }
}
