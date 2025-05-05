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
