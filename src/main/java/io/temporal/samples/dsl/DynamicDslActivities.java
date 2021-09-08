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
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;

public class DynamicDslActivities implements DynamicActivity {
  @Override
  public Object execute(EncodedValues args) {
    // Get the activity type
    String activityType = Activity.getExecutionContext().getInfo().getActivityType();
    String actionName = args.get(0, String.class);
    JsonNode workflowData = args.get(1, JsonNode.class);

    try {
      // Add a "actions" array to results to show what was executed
      if (workflowData.get("actions") != null) {
        ((ArrayNode) workflowData.get("actions")).add(actionName);
      } else {
        ((ObjectNode) workflowData).putArray("actions").add(actionName);
      }

      // Update the application status if we are approving / rejecting the customer
      // This is for demo only, in real application this would come as result of the function
      // invocation
      if (activityType.equals("Invoke Approve Application Function")) {
        ((ObjectNode) workflowData.get("customer")).put("applicationStatus", "APPROVED");
      }
      if (activityType.equals("Invoke Reject Application Function")) {
        ((ObjectNode) workflowData.get("customer")).put("applicationStatus", "REJECTED");
      }

      return workflowData;
    } catch (Exception e) {
      throw Activity.wrap(e);
    }
  }
}
