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

package io.temporal.samples.dsl.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.retry.RetryDefinition;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.utils.WorkflowUtils;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.samples.dsl.Starter;
import io.temporal.samples.dsl.Worker;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Provides utility methods for dealing with DSL */
public class DslWorkflowUtils {

  /** Set workflow options from DSL */
  public static WorkflowOptions getWorkflowOptions(Workflow workflow) {
    WorkflowOptions.Builder dslWorkflowOptionsBuilder = WorkflowOptions.newBuilder();

    if (workflow.getId() != null) {
      dslWorkflowOptionsBuilder.setWorkflowId(workflow.getId());
    }

    dslWorkflowOptionsBuilder.setTaskQueue(Worker.DEFAULT_TASK_QUEUE_NAME);

    if (workflow.getTimeouts() != null
        && workflow.getTimeouts().getWorkflowExecTimeout() != null
        && workflow.getTimeouts().getWorkflowExecTimeout().getDuration() != null) {
      dslWorkflowOptionsBuilder.setWorkflowExecutionTimeout(
          Duration.parse(workflow.getTimeouts().getWorkflowExecTimeout().getDuration()));
    }

    if (workflow.getStart() != null
        && workflow.getStart().getSchedule() != null
        && workflow.getStart().getSchedule().getCron() != null) {
      dslWorkflowOptionsBuilder.setCronSchedule(
          workflow.getStart().getSchedule().getCron().getExpression());
    }

    return dslWorkflowOptionsBuilder.build();
  }

  /** Set Activity options from DSL */
  public static ActivityOptions getActivityOptionsFromDsl(Workflow dslWorkflow) {
    ActivityOptions.Builder dslActivityOptionsBuilder = ActivityOptions.newBuilder();
    if (dslWorkflow.getTimeouts() != null
        && dslWorkflow.getTimeouts().getActionExecTimeout() != null) {
      dslActivityOptionsBuilder.setStartToCloseTimeout(
          Duration.parse(dslWorkflow.getTimeouts().getActionExecTimeout()));
    }

    // In SW spec each action (activity) can define a specific retry
    // For this demo we just use the globally defined one for all actions
    if (dslWorkflow.getRetries() != null
        && dslWorkflow.getRetries().getRetryDefs() != null
        && dslWorkflow.getRetries().getRetryDefs().size() > 0) {
      RetryDefinition retryDefinition = dslWorkflow.getRetries().getRetryDefs().get(0);
      RetryOptions.Builder dslRetryOptionsBuilder = RetryOptions.newBuilder();
      if (retryDefinition.getMaxAttempts() != null) {
        dslRetryOptionsBuilder.setMaximumAttempts(
            Integer.parseInt(retryDefinition.getMaxAttempts()));
      }
      dslRetryOptionsBuilder.setBackoffCoefficient(1.0);
      if (retryDefinition.getDelay() != null) {
        dslRetryOptionsBuilder.setInitialInterval(Duration.parse(retryDefinition.getDelay()));
      }
      if (retryDefinition.getMaxDelay() != null) {
        dslRetryOptionsBuilder.setMaximumInterval(Duration.parse(retryDefinition.getMaxDelay()));
      }
    }

    return dslActivityOptionsBuilder.build();
  }

  /** Read file and return contents as string */
  public static String getFileAsString(String fileName) throws IOException {
    File file = new File(Starter.class.getClassLoader().getResource(fileName).getFile());
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  /** Start workflow execution depending on the DSL */
  public static WorkflowExecution startWorkflow(
      WorkflowStub workflowStub, Workflow dslWorkflow, JsonNode workflowInput) {
    State startingDslWorkflowState = WorkflowUtils.getStartingState(dslWorkflow);
    if (startingDslWorkflowState instanceof EventState) {
      // This demo can parse only the first event
      EventState eventState = (EventState) startingDslWorkflowState;
      String eventName = eventState.getOnEvents().get(0).getEventRefs().get(0);

      if (eventState.getOnEvents().get(0).getActions() == null
          || eventState.getOnEvents().get(0).getActions().size() < 1) {
        return workflowStub.start(dslWorkflow.getId(), dslWorkflow.getVersion(), workflowInput);
      } else {
        // send input data as signal data
        return workflowStub.signalWithStart(
            eventName,
            new Object[] {workflowInput},
            new Object[] {dslWorkflow.getId(), dslWorkflow.getVersion()});
      }
    } else {
      // directly send input data to workflow
      return workflowStub.start(dslWorkflow.getId(), dslWorkflow.getVersion(), workflowInput);
    }
  }

  public static FunctionDefinition getFunctionDefinitionWithName(Workflow workflow, String name) {
    if (!WorkflowUtils.hasFunctionDefs(workflow)) return null;
    Optional<FunctionDefinition> funcDef =
        workflow.getFunctions().getFunctionDefs().stream()
            .filter(fd -> fd.getName().equals(name))
            .findFirst();
    return funcDef.orElse(null);
  }

  public static List<FunctionDefinition> getFunctionDefinitionsWithType(
      Workflow workflow, FunctionDefinition.Type type) {
    if (!WorkflowUtils.hasFunctionDefs(workflow)) return null;
    return workflow.getFunctions().getFunctionDefs().stream()
        .filter(fd -> fd.getType().equals(type))
        .collect(Collectors.toList());
  }

  public static JsonNode getSampleWorkflowInput(String fileName) throws Exception {
    String workflowDataInput = getFileAsString(fileName);
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(workflowDataInput);
  }
}
