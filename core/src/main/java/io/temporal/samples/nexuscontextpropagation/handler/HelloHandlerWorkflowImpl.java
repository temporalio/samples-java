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

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  public static final Logger log = Workflow.getLogger(HelloHandlerWorkflowImpl.class);

  @Override
  public NexusService.HelloOutput hello(NexusService.HelloInput input) {
    if (MDC.get("x-nexus-caller-workflow-id") != null) {
      log.info(
          "HelloHandlerWorkflow called from a workflow with ID : {}",
          MDC.get("x-nexus-caller-workflow-id"));
    }
    switch (input.getLanguage()) {
      case EN:
        return new NexusService.HelloOutput("Hello " + input.getName() + " 👋");
      case FR:
        return new NexusService.HelloOutput("Bonjour " + input.getName() + " 👋");
      case DE:
        return new NexusService.HelloOutput("Hallo " + input.getName() + " 👋");
      case ES:
        return new NexusService.HelloOutput("¡Hola! " + input.getName() + " 👋");
      case TR:
        return new NexusService.HelloOutput("Merhaba " + input.getName() + " 👋");
    }
    throw ApplicationFailure.newFailure(
        "Unsupported language: " + input.getLanguage(), "UNSUPPORTED_LANGUAGE");
  }
}
