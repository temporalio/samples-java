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

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerActivitiesImpl implements CustomerActivities {

  private static final Logger log = LoggerFactory.getLogger(CustomerActivitiesImpl.class);

  @Override
  public void getCustomerAccount(Customer customer) {
    // simulate some actual work...
    sleepSeconds(1);
  }

  @Override
  public void updateCustomerAccount(Customer customer, String message) {
    // simulate some actual work...
    sleepSeconds(1);
  }

  @Override
  public void sendUpdateEmail(Customer customer) {
    // simulate some actual work...
    sleepSeconds(1);
  }

  private void sleepSeconds(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      // This is being swallowed on purpose
      Thread.currentThread().interrupt();
      log.error("Exception in thread sleep: ", e);
    }
  }
}
