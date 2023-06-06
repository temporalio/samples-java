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

package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.failure.ApplicationFailure;
import java.util.concurrent.atomic.AtomicInteger;

public class MyActivityImpl implements MyActivity {

  /**
   * WARNING! Never rely on such shared state in real applications. The activity variables are per
   * process and in almost all cases multiple worker processes are used.
   */
  private final AtomicInteger count = new AtomicInteger();

  /** Sleeps 5 seconds. Fails for 4 first invocations, and then completes. */
  @Override
  public void execute() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (count.incrementAndGet() < 5) {
      throw ApplicationFailure.newFailure("simulated", "type1");
    }
  }
}
