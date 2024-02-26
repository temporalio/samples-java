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

import io.temporal.common.metadata.*;
import io.temporal.spring.boot.autoconfigure.template.WorkersTemplate;
import java.lang.reflect.Method;
import java.util.Map;
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

  @ReadOperation
  public String workerInfo() {
    StringBuilder sb = new StringBuilder();
    Map<String, WorkersTemplate.RegisteredInfo> registeredInfo =
        workersTemplate.getRegisteredInfo();
    sb.append("Worker Info:");
    registeredInfo.forEach(
        (taskQueue, info) -> {
          sb.append("\n\n\tTask Queue: ").append(taskQueue);
          info.getRegisteredWorkflowInfo()
              .forEach(
                  (workflowInfo) -> {
                    sb.append("\n\t\t Workflow Interface: ").append(workflowInfo.getClassName());
                    POJOWorkflowImplMetadata metadata = workflowInfo.getMetadata();
                    sb.append("\n\t\t\t Workflow Methods: ");
                    sb.append(
                        metadata.getWorkflowMethods().stream()
                            .map(POJOWorkflowMethodMetadata::getWorkflowMethod)
                            .map(Method::getName)
                            .collect(Collectors.joining(", ")));
                    sb.append("\n\t\t\t Query Methods: ");
                    sb.append(
                        metadata.getQueryMethods().stream()
                            .map(POJOWorkflowMethodMetadata::getWorkflowMethod)
                            .map(Method::getName)
                            .collect(Collectors.joining(", ")));
                    sb.append("\n\t\t\t Signal Methods: ");
                    sb.append(
                        metadata.getSignalMethods().stream()
                            .map(POJOWorkflowMethodMetadata::getWorkflowMethod)
                            .map(Method::getName)
                            .collect(Collectors.joining(", ")));
                    sb.append("\n\t\t\t Update Methods: ");
                    sb.append(
                        metadata.getUpdateMethods().stream()
                            .map(POJOWorkflowMethodMetadata::getWorkflowMethod)
                            .map(Method::getName)
                            .collect(Collectors.joining(",")));
                    sb.append("\n\t\t\t Update Validator Methods: ");
                    sb.append(
                        metadata.getUpdateValidatorMethods().stream()
                            .map(POJOWorkflowMethodMetadata::getWorkflowMethod)
                            .map(Method::getName)
                            .collect(Collectors.joining(", ")));
                  });
          info.getRegisteredActivityInfo()
              .forEach(
                  (activityInfo) -> {
                    sb.append("\n\t\t Activity Impl: ").append(activityInfo.getClassName());
                    POJOActivityImplMetadata metadata = activityInfo.getMetadata();
                    sb.append("\n\t\t\t Activity Interfaces: ");
                    sb.append(
                        metadata.getActivityInterfaces().stream()
                            .map(POJOActivityInterfaceMetadata::getInterfaceClass)
                            .map(Class::getName)
                            .collect(Collectors.joining(",")));
                    sb.append("\n\t\t\t Activity Methods: ");
                    sb.append(
                        metadata.getActivityMethods().stream()
                            .map(POJOActivityMethodMetadata::getMethod)
                            .map(Method::getName)
                            .collect(Collectors.joining(", ")));
                  });
        });

    return sb.toString();
  }
}
