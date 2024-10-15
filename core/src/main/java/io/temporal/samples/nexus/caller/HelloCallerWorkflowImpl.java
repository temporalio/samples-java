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

import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloCallerWorkflowImpl implements HelloCallerWorkflow {
  NexusService nexusService =
      Workflow.newNexusServiceStub(
          NexusService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public String hello(String message, NexusService.Language language) {
    NexusOperationHandle<NexusService.HelloOutput> handle =
        Workflow.startNexusOperation(
            nexusService::hello, new NexusService.HelloInput(message, language));
    // Optionally wait for the operation to be started. NexusOperationExecution will contain the
    // operation ID in case this operation is asynchronous.
    handle.getExecution().get();
    return handle.getResult().get().getMessage();
  }
}
