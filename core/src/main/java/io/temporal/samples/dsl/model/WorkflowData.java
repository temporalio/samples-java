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

package io.temporal.samples.dsl.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WorkflowData {

  private static final ObjectMapper mapper = new ObjectMapper();
  private ObjectNode value;

  public WorkflowData() {
    value = mapper.createObjectNode();
  }

  public WorkflowData(String data) {
    try {
      if (data == null || data.trim().length() < 1) {
        value = mapper.createObjectNode();
      } else {
        value = (ObjectNode) mapper.readTree(data);
      }
      // value.putArray("results");
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Invalid workflow data input: " + e.getMessage());
    }
  }

  public Customer getCustomer() {
    try {
      return mapper.readValue(value.get("customer").toPrettyString(), Customer.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public ObjectNode getValue() {
    return value;
  }

  public void setValue(ObjectNode value) {
    this.value = value;
  }

  public void addResults(Object result) {
    ((ArrayNode) this.value.get("results")).add(mapper.valueToTree(result));
  }

  public String valueToString() {
    return value.toPrettyString();
  }
}
