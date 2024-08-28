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

package io.temporal.samples.dsl;

import java.util.concurrent.TimeUnit;

public class DslActivitiesImpl implements DslActivities {
  @Override
  public String one() {
    sleep(1);
    return "Activity one done...";
  }

  @Override
  public String two() {
    sleep(1);
    return "Activity two done...";
  }

  @Override
  public String three() {
    sleep(1);
    return "Activity three done...";
  }

  @Override
  public String four() {
    sleep(1);
    return "Activity four done...";
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException ee) {
      // Empty
    }
  }
}
