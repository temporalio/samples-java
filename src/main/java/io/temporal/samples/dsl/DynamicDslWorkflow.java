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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.branches.Branch;
import io.serverlessworkflow.api.events.OnEvents;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.functions.SubFlowRef;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.*;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.utils.WorkflowUtils;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.common.converter.EncodedValues;
import io.temporal.samples.dsl.model.ActResult;
import io.temporal.samples.dsl.model.WorkflowData;
import io.temporal.samples.dsl.utils.DslWorkflowUtils;
import io.temporal.samples.dsl.utils.JQFilter;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class DynamicDslWorkflow implements DynamicWorkflow {
  private static final Logger logger = Workflow.getLogger(DynamicDslWorkflow.class);

  private io.serverlessworkflow.api.Workflow dslWorkflow;
  private WorkflowData workflowData = new WorkflowData();
  private List<FunctionDefinition> queryFunctions;
  private Map<String, WorkflowData> signalMap = new HashMap<>();

  private ActivityStub activities;

  @Override
  public Object execute(EncodedValues args) {
    // Get first input and convert to SW Workflow object
    String dslWorkflowId = args.get(0, String.class);
    String dslWorkflowVersion = args.get(1, String.class);
    // Get second input which is set to workflowData
    workflowData.setValue((ObjectNode) args.get(2, JsonNode.class));

    // Using a global shared workflow object here is only allowed because its
    // assumed that at this point it is immutable and the same across all workflow worker restarts
    dslWorkflow = DslWorkflowCache.getWorkflow(dslWorkflowId, dslWorkflowVersion);

    // Get all expression type functions to be used for queries
    queryFunctions =
        DslWorkflowUtils.getFunctionDefinitionsWithType(
            dslWorkflow, FunctionDefinition.Type.EXPRESSION);

    // Register dynamic signal handler
    // For demo signals input sets the workflowData
    // Improvement can be to add to it instead
    Workflow.registerListener(
        (DynamicSignalHandler)
            (signalName, encodedArgs) -> {
              if (workflowData == null) {
                workflowData = new WorkflowData();
              }
              workflowData.setValue((ObjectNode) encodedArgs.get(0, JsonNode.class));
              signalMap.put(signalName, workflowData);
            });

    // Register dynamic query handler
    // we use expression type functions in workflow def as query definitions
    Workflow.registerListener(
        (DynamicQueryHandler)
            (queryType, encodedArgs) -> {
              if (queryFunctions == null
                  || DslWorkflowUtils.getFunctionDefinitionWithName(dslWorkflow, queryType)
                      == null) {
                logger.warn("Unable to find expression function with name: " + queryType);
                String queryInput = encodedArgs.get(0, String.class);
                if (queryInput == null || queryInput.length() < 1) {
                  // no input just return workflow data
                  return workflowData.getValue();
                } else {
                  return JQFilter.getInstance()
                      .evaluateExpression(queryInput, workflowData.getValue());
                }
              }
              return JQFilter.getInstance()
                  .evaluateExpression(
                      DslWorkflowUtils.getFunctionDefinitionWithName(dslWorkflow, queryType)
                          .getOperation(),
                      workflowData.getValue());
            });

    // Get the activity options that are set from properties in dsl
    ActivityOptions activityOptions = DslWorkflowUtils.getActivityOptionsFromDsl(dslWorkflow);
    // Create a dynamic activities stub to be used for all actions in dsl
    activities = Workflow.newUntypedActivityStub(activityOptions);

    // Start going through the dsl workflow states and execute depending on their instructions
    executeDslWorkflowFrom(WorkflowUtils.getStartingState(dslWorkflow));

    // Return the final workflow data as result
    return workflowData.getValue();
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

        if (eventState.getOnEvents().get(0).getActions() == null
            || eventState.getOnEvents().get(0).getActions().size() < 1) {
          // no actions..assume we are just waiting on event here
          Workflow.await(
              () -> signalMap.containsKey(eventState.getOnEvents().get(0).getEventRefs().get(0)));
          workflowData = signalMap.get(eventState.getOnEvents().get(0).getEventRefs().get(0));
        } else {
          List<Action> eventStateActions = eventState.getOnEvents().get(0).getActions();
          if (eventState.getOnEvents().get(0).getActionMode() != null
              && eventState
                  .getOnEvents()
                  .get(0)
                  .getActionMode()
                  .equals(OnEvents.ActionMode.PARALLEL)) {
            List<Promise<ActResult>> eventPromises = new ArrayList<>();

            for (Action action : eventStateActions) {
              eventPromises.add(
                  activities.executeAsync(
                      action.getFunctionRef().getRefName(),
                      ActResult.class,
                      workflowData.getCustomer()));
            }
            // Invoke all activities in parallel. Wait for all to complete
            Promise.allOf(eventPromises).get();

            for (Promise<ActResult> promise : eventPromises) {
              workflowData.addResults(promise.get());
            }
          } else {
            for (Action action : eventStateActions) {
              if (action.getSleep() != null && action.getSleep().getBefore() != null) {
                Workflow.sleep(Duration.parse(action.getSleep().getBefore()));
              }
              // execute the action as an activity and assign its results to workflowData
              workflowData.addResults(
                  activities.execute(
                      action.getFunctionRef().getRefName(),
                      ActResult.class,
                      workflowData.getCustomer()));
              if (action.getSleep() != null && action.getSleep().getAfter() != null) {
                Workflow.sleep(Duration.parse(action.getSleep().getAfter()));
              }
            }
          }
        }
      }
      if (eventState.getTransition() == null || eventState.getTransition().getNextState() == null) {
        return null;
      }
      return WorkflowUtils.getStateWithName(dslWorkflow, eventState.getTransition().getNextState());

    } else if (dslWorkflowState instanceof OperationState) {
      OperationState operationState = (OperationState) dslWorkflowState;

      if (operationState.getActions() != null && operationState.getActions().size() > 0) {
        // Check if actions should be executed sequentially or parallel
        if (operationState.getActionMode() != null
            && operationState.getActionMode().equals(OperationState.ActionMode.PARALLEL)) {
          List<Promise<ActResult>> actionsPromises = new ArrayList<>();

          for (Action action : operationState.getActions()) {
            actionsPromises.add(
                activities.executeAsync(
                    action.getFunctionRef().getRefName(),
                    ActResult.class,
                    workflowData.getCustomer()));
          }
          // Invoke all activities in parallel. Wait for all to complete
          Promise.allOf(actionsPromises).get();

          for (Promise<ActResult> promise : actionsPromises) {
            workflowData.addResults(promise.get());
          }
        } else {
          for (Action action : operationState.getActions()) {
            // added support for subflow (child workflow)
            if (action.getSubFlowRef() != null) {

              if (action.getSubFlowRef().getInvoke() != null
                  && action.getSubFlowRef().getInvoke().equals(SubFlowRef.Invoke.ASYNC)) {
                ChildWorkflowOptions childWorkflowOptions;

                if (action
                    .getSubFlowRef()
                    .getOnParentComplete()
                    .equals(SubFlowRef.OnParentComplete.CONTINUE)) {
                  childWorkflowOptions =
                      ChildWorkflowOptions.newBuilder()
                          .setWorkflowId(action.getSubFlowRef().getWorkflowId())
                          .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                          .build();
                } else {
                  childWorkflowOptions =
                      ChildWorkflowOptions.newBuilder()
                          .setWorkflowId(action.getSubFlowRef().getWorkflowId())
                          .build();
                }
                ChildWorkflowStub childWorkflow =
                    Workflow.newUntypedChildWorkflowStub(
                        action.getSubFlowRef().getWorkflowId(), childWorkflowOptions);
                childWorkflow.executeAsync(
                    Object.class,
                    action.getSubFlowRef().getWorkflowId(),
                    action.getSubFlowRef().getVersion(),
                    workflowData.getValue());
                // for async we do not care about result in sample
                // wait until child starts
                Promise<WorkflowExecution> childExecution =
                    Workflow.getWorkflowExecution(childWorkflow);
                childExecution.get();

              } else {
                ChildWorkflowStub childWorkflow =
                    Workflow.newUntypedChildWorkflowStub(
                        action.getSubFlowRef().getWorkflowId(),
                        ChildWorkflowOptions.newBuilder()
                            .setWorkflowId(action.getSubFlowRef().getWorkflowId())
                            .build());

                workflowData.addResults(
                    childWorkflow.execute(
                        Object.class,
                        action.getSubFlowRef().getWorkflowId(),
                        action.getSubFlowRef().getVersion(),
                        workflowData.getValue()));
              }
            } else {
              // check if its a custom function
              FunctionDefinition functionDefinition =
                  WorkflowUtils.getFunctionDefinitionsForAction(dslWorkflow, action.getName());
              if (functionDefinition.getType().equals(FunctionDefinition.Type.CUSTOM)) {
                // for this example custom function is assumed sending signal via external stub
                String[] operationParts = functionDefinition.getOperation().split("#", -1);
                ExternalWorkflowStub externalWorkflowStub =
                    Workflow.newUntypedExternalWorkflowStub(operationParts[0]);
                externalWorkflowStub.signal(operationParts[1], workflowData.getValue());
              } else {
                if (action.getSleep() != null && action.getSleep().getBefore() != null) {
                  Workflow.sleep(Duration.parse(action.getSleep().getBefore()));
                }
                // execute the action as an activity and assign its results to workflowData
                workflowData.addResults(
                    activities.execute(
                        action.getFunctionRef().getRefName(),
                        ActResult.class,
                        workflowData.getCustomer()));

                if (action.getSleep() != null && action.getSleep().getAfter() != null) {
                  Workflow.sleep(Duration.parse(action.getSleep().getAfter()));
                }
              }
            }
          }
        }
      }
      if (operationState.getTransition() == null
          || operationState.getTransition().getNextState() == null) {
        return null;
      }
      return WorkflowUtils.getStateWithName(
          dslWorkflow, operationState.getTransition().getNextState());
    } else if (dslWorkflowState instanceof SwitchState) {
      // Demo supports only data based switch
      SwitchState switchState = (SwitchState) dslWorkflowState;
      if (switchState.getDataConditions() != null && switchState.getDataConditions().size() > 0) {
        // evaluate each condition to see if its true. If none are true default to defaultCondition
        for (DataCondition dataCondition : switchState.getDataConditions()) {
          if (JQFilter.getInstance()
              .evaluateBooleanExpression(dataCondition.getCondition(), workflowData.getValue())) {
            if (dataCondition.getTransition() == null
                || dataCondition.getTransition().getNextState() == null) {
              return null;
            }
            return WorkflowUtils.getStateWithName(
                dslWorkflow, dataCondition.getTransition().getNextState());
          }
        }
        // no conditions evaluated to true, use default condition
        if (switchState.getDefaultCondition().getTransition() == null) {
          return null;
        }
        return WorkflowUtils.getStateWithName(
            dslWorkflow, switchState.getDefaultCondition().getTransition().getNextState());
      } else {
        // no conditions use the transition/end of default condition
        if (switchState.getDefaultCondition().getTransition() == null) {
          return null;
        }
        return WorkflowUtils.getStateWithName(
            dslWorkflow, switchState.getDefaultCondition().getTransition().getNextState());
      }
    } else if (dslWorkflowState instanceof SleepState) {
      SleepState sleepState = (SleepState) dslWorkflowState;
      if (sleepState.getDuration() != null) {
        Workflow.sleep(Duration.parse(sleepState.getDuration()));
      }
      if (sleepState.getTransition() == null || sleepState.getTransition().getNextState() == null) {
        return null;
      }
      return WorkflowUtils.getStateWithName(dslWorkflow, sleepState.getTransition().getNextState());
    } else if (dslWorkflowState instanceof ForEachState) {
      ForEachState state = (ForEachState) dslWorkflowState;
      // List<Promise<JsonNode>> actionsPromises = new ArrayList<>();

      List<JsonNode> inputs =
          JQFilter.getInstance()
              .evaluateArrayExpression(state.getInputCollection(), workflowData.getValue());
      // TODO: update to exec all in parallel!
      for (JsonNode ignored : inputs) {
        for (Action action : state.getActions()) {
          if (action.getSleep() != null && action.getSleep().getBefore() != null) {
            Workflow.sleep(Duration.parse(action.getSleep().getBefore()));
          }

          // execute the action as an activity and assign its results to workflowData
          workflowData.addResults(
              activities.execute(
                  action.getFunctionRef().getRefName(),
                  ActResult.class,
                  workflowData.getCustomer()));

          if (action.getSleep() != null && action.getSleep().getAfter() != null) {
            Workflow.sleep(Duration.parse(action.getSleep().getAfter()));
          }
        }
      }

      if (state.getTransition() == null || state.getTransition().getNextState() == null) {
        return null;
      }

      return WorkflowUtils.getStateWithName(dslWorkflow, state.getTransition().getNextState());
    } else if (dslWorkflowState instanceof ParallelState) {
      ParallelState parallelState = (ParallelState) dslWorkflowState;

      // this is just initial impl, still need to add things like timeouts etc
      // also this currently assumes the "allof" completion type (default)
      if (parallelState.getBranches() != null && parallelState.getBranches().size() > 0) {
        List<Promise<Void>> branchAllOfPromises = new ArrayList<>();

        for (Branch branch : parallelState.getBranches()) {
          branchAllOfPromises.add(Async.procedure(this::processBranchActions, branch));
        }

        // execute all branch actions in parallel..wait for all to complete
        Promise.allOf(branchAllOfPromises).get();
      }

      if (parallelState.getTransition() == null
          || parallelState.getTransition().getNextState() == null) {
        return null;
      }

      return WorkflowUtils.getStateWithName(
          dslWorkflow, parallelState.getTransition().getNextState());
    } else {
      logger.error("Invalid or unsupported in demo dsl workflow state: " + dslWorkflowState);
      return null;
    }
  }

  private void processBranchActions(Branch branch) {
    // here we assume for now that all actions themselves inside
    // branch are also executed in parallel, just for sample sake
    // we should check the action mode to see if its sequential or parallel
    // will add...
    List<Promise<ActResult>> branchActionPromises = new ArrayList<>();
    List<Action> branchActions = branch.getActions();
    for (Action action : branchActions) {
      branchActionPromises.add(
          activities.executeAsync(
              action.getFunctionRef().getRefName(), ActResult.class, workflowData.getCustomer()));
    }

    Promise.allOf(branchActionPromises).get();

    for (Promise<ActResult> promise : branchActionPromises) {
      workflowData.addResults(promise.get());
    }
  }
}
