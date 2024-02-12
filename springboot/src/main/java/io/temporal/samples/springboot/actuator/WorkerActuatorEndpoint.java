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

package io.temporal.samples.springboot.actuator;

import io.temporal.spring.boot.autoconfigure.template.WorkersTemplate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "temporalworkerinfo")
public class WorkerActuatorEndpoint {
  @Autowired
  @Qualifier("temporalWorkersTemplate")
  private WorkersTemplate workersTemplate;

  private AtomicInteger counter = new AtomicInteger(0);

  @ReadOperation
  public String workerInfo() {
    StringBuilder sb = new StringBuilder();
    Map<String, WorkersTemplate.RegisteredInfo> registeredInfo =
        workersTemplate.getRegisteredInfo();
    sb.append("Worker Info:\n\n");
    registeredInfo.forEach(
        (taskQueue, info) ->
            sb.append(counter.incrementAndGet())
                .append(".")
                .append("\tTask Queue: ")
                .append(taskQueue)
                .append("\n\tRegistered Workflow Impls: ")
                .append(info.getRegisteredWorkflowInfo().stream().collect(Collectors.joining(",")))
                .append("\n\tRegistered Activity Impls: ")
                .append(info.getRegisteredActivityInfo().stream().collect(Collectors.joining(",")))
                .append("\n\n"));

    return sb.toString();
  }
}
