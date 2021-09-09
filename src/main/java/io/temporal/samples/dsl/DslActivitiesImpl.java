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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.temporal.activity.Activity;

public class DslActivitiesImpl implements DslActivities {
  @Override
  public JsonNode checkCustomerInfo(JsonNode workflowData) {
    return updateWorkflowDataActions(
        workflowData, Activity.getExecutionContext().getInfo().getActivityType());
  }

  @Override
  public JsonNode approveApplication(JsonNode workflowData) {
    workflowData =
        updateWorkflowDataActions(
            workflowData, Activity.getExecutionContext().getInfo().getActivityType());
    // Simulates invocation of a rest function, just adds APPROVED status
    return updateApplicationStatus(workflowData, "APPROVED");
  }

  @Override
  public JsonNode rejectApplication(JsonNode workflowData) {
    updateWorkflowDataActions(
        workflowData, Activity.getExecutionContext().getInfo().getActivityType());

    return updateApplicationStatus(workflowData, "DENIED");
  }

  private JsonNode updateWorkflowDataActions(JsonNode workflowData, String activityType) {
    // Add a "actions" array to results to show what was executed
    if (workflowData.get("actions") != null) {
      ((ArrayNode) workflowData.get("actions")).add(activityType);
    } else {
      ((ObjectNode) workflowData).putArray("actions").add(activityType);
    }

    return workflowData;
  }

  private JsonNode updateApplicationStatus(JsonNode workflowData, String status) {
    ((ObjectNode) workflowData.get("customer")).put("applicationStatus", status);
    return workflowData;
  }
}
