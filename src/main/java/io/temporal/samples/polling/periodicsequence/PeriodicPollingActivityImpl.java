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

package io.temporal.samples.polling.periodicsequence;

import io.temporal.activity.Activity;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.TestService;

public class PeriodicPollingActivityImpl implements PollingActivities {

  private TestService service;

  public PeriodicPollingActivityImpl(TestService service) {
    this.service = service;
  }

  @Override
  public String doPoll() {
    try {
      return service.getServiceResult();
    } catch (TestService.TestServiceException e) {
      // We want to rethrow the service exception so we can poll via activity retries
      throw Activity.wrap(e);
    }
  }
}
