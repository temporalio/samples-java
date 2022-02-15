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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.module.loaders.BuiltinModuleLoader;

public class JQFilter {

  private static final Scope rootScope = Scope.newEmptyScope();
  private static Map<String, JsonQuery> expressionMap = new HashMap<>();

  private static volatile JQFilter instance;

  public static JQFilter getInstance() {
    if (instance == null) {
      synchronized (JQFilter.class) {
        if (instance == null) {
          instance = new JQFilter();
        }
      }
    }
    return instance;
  }

  private JQFilter() {
    BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);
    rootScope.setModuleLoader(BuiltinModuleLoader.getInstance());
  }

  public JsonNode evaluateExpression(String expression, JsonNode data) {

    try {

      final Scope childScope = Scope.newChildScope(rootScope);
      final List<JsonNode> result = new ArrayList<>();

      final String toEvalExpression = expression.replace("${", "").replaceAll("}$", "");

      if (!expressionMap.containsKey(toEvalExpression)) {
        expressionMap.put(toEvalExpression, JsonQuery.compile(toEvalExpression, Versions.JQ_1_6));
      }

      expressionMap.get(toEvalExpression).apply(childScope, data, result::add);

      return result.get(0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean evaluateBooleanExpression(String expression, JsonNode data) {
    BooleanNode result = (BooleanNode) evaluateExpression(expression, data);
    return result.booleanValue();
  }

  public List<JsonNode> evaluateArrayExpression(String expression, JsonNode data) {
    ArrayNode result = (ArrayNode) evaluateExpression(expression, data);
    List<JsonNode> resultList = new ArrayList<>(result.size());
    result.forEach(jsonNode -> resultList.add(jsonNode));

    return resultList;
  }
}
