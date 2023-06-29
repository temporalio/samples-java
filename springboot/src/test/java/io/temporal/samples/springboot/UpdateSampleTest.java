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

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.client.*;
import io.temporal.samples.springboot.update.PurchaseWorkflow;
import io.temporal.samples.springboot.update.model.Purchase;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = HelloSampleTest.Configuration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// set this to omit setting up embedded kafka
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@DirtiesContext
public class UpdateSampleTest {

  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

  @BeforeEach
  void setUp() {
    applicationContext.start();
  }

  @Test
  public void testUpdate() {
    PurchaseWorkflow workflow =
        workflowClient.newWorkflowStub(
            PurchaseWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("UpdateSampleTaskQueue")
                .setWorkflowId("NewPurchase")
                .build());
    Purchase purchase = new Purchase(1, 3);
    WorkflowClient.start(workflow::start);
    // send update
    workflow.makePurchase(purchase);
    workflow.exit();
    WorkflowStub.fromTyped(workflow).getResult(Void.class);
  }

  @Test()
  public void testUpdateRejected() {
    PurchaseWorkflow workflow =
        workflowClient.newWorkflowStub(
            PurchaseWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("UpdateSampleTaskQueue")
                .setWorkflowId("NewPurchase")
                .build());
    Purchase purchase = new Purchase(1, 40);
    WorkflowClient.start(workflow::start);
    // send update
    assertThrows(
        WorkflowUpdateException.class,
        () -> {
          workflow.makePurchase(purchase);
        });
    workflow.exit();
    WorkflowStub.fromTyped(workflow).getResult(Void.class);
  }

  @ComponentScan
  public static class Configuration {}
}
