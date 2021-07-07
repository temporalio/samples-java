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

package io.temporal.samples.listworkflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Optional;

public class CustomerWorkflowImpl implements CustomerWorkflow {
  private boolean exit;
  private final CustomerActivities customerActivities =
      Workflow.newActivityStub(
          CustomerActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
  private final RetryOptions customerRetryOptions =
      RetryOptions.newBuilder().setMaximumAttempts(5).build();
  private final Duration expiration = Duration.ofMinutes(1);

  @Override
  public void updateAccountMessage(Customer customer, String message) {

    Workflow.retry(
        customerRetryOptions,
        Optional.of(expiration),
        () -> {
          customerActivities.getCustomerAccount(customer);
          customerActivities.updateCustomerAccount(customer, message);
          customerActivities.sendUpdateEmail(customer);
        });

    Workflow.await(Duration.ofMinutes(1), () -> exit);
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
