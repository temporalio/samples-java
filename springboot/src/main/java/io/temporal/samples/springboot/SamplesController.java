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

package io.temporal.samples.springboot;

import io.grpc.StatusRuntimeException;
import io.temporal.api.enums.v1.TaskQueueKind;
import io.temporal.api.enums.v1.TaskQueueType;
import io.temporal.api.taskqueue.v1.TaskQueue;
import io.temporal.api.workflowservice.v1.DescribeTaskQueueRequest;
import io.temporal.api.workflowservice.v1.DescribeTaskQueueResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.samples.springboot.customize.CustomizeWorkflow;
import io.temporal.samples.springboot.hello.HelloWorkflow;
import io.temporal.samples.springboot.hello.model.Person;
import io.temporal.samples.springboot.kafka.MessageWorkflow;
import io.temporal.samples.springboot.update.PurchaseWorkflow;
import io.temporal.samples.springboot.update.model.ProductRepository;
import io.temporal.samples.springboot.update.model.Purchase;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SamplesController {

  @Autowired WorkflowClient client;

  @Autowired ProductRepository productRepository;

  @GetMapping("/hello")
  public String hello(Model model) {
    model.addAttribute("sample", "Say Hello");
    return "hello";
  }

  @PostMapping(
      value = "/hello",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.TEXT_HTML_VALUE})
  ResponseEntity helloSample(@RequestBody Person person) {
    HelloWorkflow workflow =
        client.newWorkflowStub(
            HelloWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("HelloSampleTaskQueue")
                .setWorkflowId("HelloSample")
                .build());

    // bypass thymeleaf, don't return template name just result
    return new ResponseEntity<>("\"" + workflow.sayHello(person) + "\"", HttpStatus.OK);
  }

  @GetMapping("/metrics")
  public String metrics(Model model) {
    model.addAttribute("sample", "SDK Metrics");
    return "metrics";
  }

  @GetMapping("/update")
  public String update(Model model) {
    model.addAttribute("sample", "Synchronous Update");
    model.addAttribute("products", productRepository.findAll());
    return "update";
  }

  @GetMapping("/update/inventory")
  public String updateInventory(Model model) {
    model.addAttribute("products", productRepository.findAll());
    return "update :: inventory";
  }

  @PostMapping(
      value = "/update/purchase",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.TEXT_HTML_VALUE})
  ResponseEntity purchase(@RequestBody Purchase purchase) {
    PurchaseWorkflow workflow =
        client.newWorkflowStub(
            PurchaseWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("UpdateSampleTaskQueue")
                .setWorkflowId("NewPurchase")
                .build());
    WorkflowClient.start(workflow::start);

    // send update
    try {
      boolean isValidPurchase = workflow.makePurchase(purchase);
      // for sample send exit to workflow exec and wait till it completes
      workflow.exit();
      WorkflowStub.fromTyped(workflow).getResult(Void.class);
      if (!isValidPurchase) {
        return new ResponseEntity<>("\"Invalid purchase\"", HttpStatus.NOT_FOUND);
      }
      return new ResponseEntity<>("\"" + "Purchase successful" + "\"", HttpStatus.OK);
    } catch (WorkflowUpdateException | StatusRuntimeException e) {
      // for sample send exit to workflow exec and wait till it completes
      workflow.exit();
      WorkflowStub.fromTyped(workflow).getResult(Void.class);

      String message = e.getMessage();
      if (e instanceof WorkflowUpdateException) {
        message = e.getCause().getMessage();
      }

      return new ResponseEntity<>("\"" + message + "\"", HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/kafka")
  public String kafka(Model model) {
    model.addAttribute("sample", "Kafka Request / Reply");
    return "kafka";
  }

  @PostMapping(
      value = "/kafka",
      consumes = {MediaType.TEXT_PLAIN_VALUE},
      produces = {MediaType.TEXT_HTML_VALUE})
  ResponseEntity sendToKafka(@RequestBody String message) {
    MessageWorkflow workflow =
        client.newWorkflowStub(
            MessageWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("KafkaSampleTaskQueue")
                .setWorkflowId("MessageSample")
                .build());

    WorkflowClient.start(workflow::start);
    workflow.update(message);

    // wait till exec completes
    WorkflowStub.fromTyped(workflow).getResult(Void.class);
    // bypass thymeleaf, don't return template name just result
    return new ResponseEntity<>("\" Message workflow completed\"", HttpStatus.OK);
  }

  @GetMapping("/customize")
  public String customize(Model model) {
    model.addAttribute("sample", "Customizing Options");
    return "customize";
  }

  @PostMapping(
      value = "/customize",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.TEXT_HTML_VALUE})
  ResponseEntity customizeSample() {
    CustomizeWorkflow workflow =
        client.newWorkflowStub(
            CustomizeWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("CustomizeTaskQueue")
                .setWorkflowId("CustomizeSample")
                .build());

    // bypass thymeleaf, don't return template name just result
    return new ResponseEntity<>("\"" + workflow.execute() + "\"", HttpStatus.OK);
  }

  @GetMapping("/camel")
  public String camel(Model model) {
    model.addAttribute("sample", "Camel Route");
    return "camel";
  }

  @GetMapping("/customendpoint")
  public String customEndpoint(Model model) {
    model.addAttribute("sample", "Custom Actuator Worker Info Endpoint");
    return "actuator";
  }

  @GetMapping("/health")
  public String health() {
    getQueueStatus();
    return "up";
  }

  public void getQueueStatus() {

    TaskQueue tq =
        TaskQueue.newBuilder()
            .setKind(TaskQueueKind.TASK_QUEUE_KIND_UNSPECIFIED)
            .setName("HelloSampleTaskQueue")
            .build();

    DescribeTaskQueueRequest taskQrqst =
        DescribeTaskQueueRequest.newBuilder()
            .setNamespace("default")
            .setTaskQueue(tq)
            .setIncludeTaskQueueStatus(true)
            .setTaskQueueType(TaskQueueType.TASK_QUEUE_TYPE_ACTIVITY)
            .build();

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newConnectedServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget("localhost:7233").build(), null);

    DescribeTaskQueueResponse activityResponse =
        service.blockingStub().describeTaskQueue(taskQrqst);
    System.out.println(activityResponse);

    taskQrqst =
        DescribeTaskQueueRequest.newBuilder(taskQrqst)
            .setTaskQueueType(TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW)
            .build();

    DescribeTaskQueueResponse wfResponse = service.blockingStub().describeTaskQueue(taskQrqst);
    System.out.println(wfResponse);
  }
}
