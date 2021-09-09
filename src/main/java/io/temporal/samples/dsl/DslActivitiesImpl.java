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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;

public class DslActivitiesImpl implements DslActivities {
  @Override
  public JsonNode checkCustomerInfo() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(
          getReturnJson(Activity.getExecutionContext().getInfo().getActivityType(), "invoked"));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public JsonNode updateApplicationInfo() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(
          getReturnJson(Activity.getExecutionContext().getInfo().getActivityType(), "invoked"));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public JsonNode approveApplication() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(getReturnJson("decision", "APPROVED"));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public JsonNode rejectApplication() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(getReturnJson("decision", "DENIED"));
    } catch (Exception e) {
      return null;
    }
  }

  private String getReturnJson(String key, String value) {
    return "{\n" + "  \"" + key + "\": \"" + value + "\"\n" + "}";
  }
}
