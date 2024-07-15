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

package io.temporal.samples.polling;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Test service that we want to poll. It simulates a service being down and then returning a result
 * after 5 attempts
 */
public class TestService {
  private int tryAttempt = 0;
  private int errorAttempts = 5; // default to 5 attempts before returning result
  private boolean doRetryAfter = false;
  private int minRetryAfter = 1;
  private int maxRetryAfter = 3;

  public TestService() {}

  public TestService(int errorAttempts) {
    this.errorAttempts = errorAttempts;
  }

  public TestService(int errorAttempts, boolean doRetryAfter) {
    this.errorAttempts = errorAttempts;
    this.doRetryAfter = doRetryAfter;
  }

  public String getServiceResult() throws TestServiceException {
    tryAttempt++;
    if (tryAttempt % errorAttempts == 0) {
      return "OK";
    } else {
      if (!doRetryAfter) {
        throw new TestServiceException("Service is down");
      } else {
        throw new TestServiceException(
            "Service is down",
            ThreadLocalRandom.current().nextInt(minRetryAfter, maxRetryAfter + 1));
      }
    }
  }

  public static class TestServiceException extends Exception {
    private int retryAfterInMinutes = 1;

    public TestServiceException(String message) {
      super(message);
    }

    public TestServiceException(String message, int retryAfterInMinutes) {
      super(message);
      this.retryAfterInMinutes = retryAfterInMinutes;
    }

    public int getRetryAfterInMinutes() {
      return retryAfterInMinutes;
    }
  }
}
