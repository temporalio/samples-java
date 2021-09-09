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

package io.temporal.samples.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Workflow;
import java.util.List;
import org.slf4j.Logger;

public class DynamicDslWorkflow implements DynamicWorkflow {

  private io.serverlessworkflow.api.Workflow dslWorkflow;
  private JsonNode workflowData;
  private Logger logger = Workflow.getLogger(DynamicDslWorkflow.class);
  ActivityStub activities;

  @Override
  public Object execute(EncodedValues args) {
    // Get first input and convert to SW Workflow object
    String dsl = args.get(0, String.class);
    dslWorkflow = io.serverlessworkflow.api.Workflow.fromSource(dsl);
    // Get second input which is set to workflowData
    workflowData = args.get(1, JsonNode.class);

    // Register dynamic signal handler
    // For demo signals input sets the workflowData
    // Improvement can be to add to it instead
    Workflow.registerListener(
        (DynamicSignalHandler)
            (signalName, encodedArgs) -> workflowData = encodedArgs.get(0, JsonNode.class));

    // Get the activity options that are set from properties in dsl
    ActivityOptions activityOptions = DslWorkflowUtils.getActivityOptionsFromDsl(dslWorkflow);
    // Create a dynamic activities stub to be used for all actions in dsl
    activities = Workflow.newUntypedActivityStub(activityOptions);

    // Start going through the dsl workflow states and execute depending on their instructions
    executeDslWorkflowFrom(DslWorkflowUtils.getStartingWorkflowState(dslWorkflow));

    // Return the final workflow data as result
    return workflowData;
  }

  /** Executes workflow according to the dsl control flow logic */
  private void executeDslWorkflowFrom(State dslWorkflowState) {
    // This demo supports 3 states: Event State, Operation State and Switch state (data-based
    // switch)
    if (dslWorkflowState != null) {
      // execute the state and return the next workflow state depending on control flow logic in dsl
      // if next state is null it means that we need to stop execution
      executeDslWorkflowFrom(executeStateAndReturnNext(dslWorkflowState));
    } else {
      // done
      return;
    }
  }

  /**
   * Executes the control flow logic for a dsl workflow state. Demo supports EventState,
   * OperationState, and SwitchState currently. More can be added.
   */
  private State executeStateAndReturnNext(State dslWorkflowState) {
    if (dslWorkflowState instanceof EventState) {
      EventState eventState = (EventState) dslWorkflowState;
      // currently this demo supports only the first onEvents
      if (eventState.getOnEvents() != null && eventState.getOnEvents().size() > 0) {
        List<Action> eventStateActions = eventState.getOnEvents().get(0).getActions();
        for (Action action : eventStateActions) {
          // execute the action as an activity and assign its results to workflowData
          workflowData =
              activities.execute(
                  action.getFunctionRef().getRefName(), JsonNode.class, workflowData);
        }
      }
      if (eventState.getTransition() == null || eventState.getTransition().getNextState() == null) {
        return null;
      }
      return DslWorkflowUtils.getWorkflowStateWithName(
          eventState.getTransition().getNextState(), dslWorkflow);

    } else if (dslWorkflowState instanceof OperationState) {
      OperationState operationState = (OperationState) dslWorkflowState;
      if (operationState.getActions() != null && operationState.getActions().size() > 0) {
        for (Action action : operationState.getActions()) {
          // execute the action as an activity and assign its results to workflowData
          workflowData =
              activities.execute(
                  action.getFunctionRef().getRefName(), JsonNode.class, workflowData);
        }
      }
      if (operationState.getTransition() == null
          || operationState.getTransition().getNextState() == null) {
        return null;
      }
      return DslWorkflowUtils.getWorkflowStateWithName(
          operationState.getTransition().getNextState(), dslWorkflow);
    } else if (dslWorkflowState instanceof SwitchState) {
      // Demo supports only data based switch
      SwitchState switchState = (SwitchState) dslWorkflowState;
      if (switchState.getDataConditions() != null && switchState.getDataConditions().size() > 0) {
        // evaluate each condition to see if its true. If none are true default to defaultCondition
        for (DataCondition dataCondition : switchState.getDataConditions()) {
          if (DslWorkflowUtils.isTrueDataCondition(
              dataCondition.getCondition(), workflowData.toPrettyString())) {
            if (dataCondition.getTransition() == null
                || dataCondition.getTransition().getNextState() == null) {
              return null;
            }
            return DslWorkflowUtils.getWorkflowStateWithName(
                dataCondition.getTransition().getNextState(), dslWorkflow);
          }
        }
        // no conditions evaluated to true, use default condition
        if (switchState.getDefaultCondition().getTransition() == null) {
          return null;
        }
        return DslWorkflowUtils.getWorkflowStateWithName(
            switchState.getDefaultCondition().getTransition().getNextState(), dslWorkflow);
      } else {
        // no conditions use the transition/end of default condition
        if (switchState.getDefaultCondition().getTransition() == null) {
          return null;
        }
        return DslWorkflowUtils.getWorkflowStateWithName(
            switchState.getDefaultCondition().getTransition().getNextState(), dslWorkflow);
      }
    } else {
      logger.error("Invalid or unsupported in demo dsl workflow state: " + dslWorkflowState);
      return null;
    }
  }
}
