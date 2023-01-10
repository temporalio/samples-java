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

import com.google.common.base.Throwables;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;

public class RetryOnSignalWorkflowOutboundCallsInterceptor
    extends WorkflowOutboundCallsInterceptorBase {

  private enum Action {
    RETRY,
    FAIL
  }

  private class ActivityRetryState<R> {
    private final ActivityInput<R> input;

    private Action action;
    private ActivityFailure lastFailure;
    private int attempt;

    private ActivityRetryState(ActivityInput<R> input) {
      this.input = input;
    }

    ActivityOutput<R> execute() {
      while (true) {
        try {
          action = null;
          lastFailure = null;
          attempt++;
          return RetryOnSignalWorkflowOutboundCallsInterceptor.super.executeActivity(input);
        } catch (ActivityFailure e) {
          lastFailure = e;
          Workflow.await(() -> action != null);
          if (action == Action.FAIL) {
            throw e;
          }
        }
      }
    }

    public void retry() {
      if (lastFailure == null) {
        return;
      }
      action = Action.RETRY;
    }

    public void fail() {
      if (lastFailure == null) {
        return;
      }
      action = Action.FAIL;
    }

    public String getStatus() {
      String activityName = input.getActivityName();
      if (lastFailure == null) {
        return "Executing activity " + activityName + " " + attempt + " time";
      }
      if (action == null) {
        return "Last "
            + activityName
            + " activity failure:\n"
            + Throwables.getStackTraceAsString(lastFailure)
            + "\n\nretry or fail ?";
      }
      return (action == Action.RETRY ? "Going to retry" : "Going to fail")
          + " activity "
          + activityName;
    }
  }

  /**
   * For the example brevity the interceptor fails or retries all activities that are waiting for an
   * action. The production version might implement retry and failure of specific activities by
   * their type.
   */
  private final List<ActivityRetryState<?>> pendingActivities = new ArrayList<>();

  public RetryOnSignalWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
    super(next);
    Workflow.registerListener(
        new RetryOnSignalInterceptorListener() {
          @Override
          public void retry() {
            for (ActivityRetryState<?> pending : pendingActivities) {
              pending.retry();
            }
          }

          @Override
          public void fail() {
            for (ActivityRetryState<?> pending : pendingActivities) {
              pending.fail();
            }
          }

          @Override
          public String getPendingActivitiesStatus() {
            StringBuffer result = new StringBuffer();
            for (ActivityRetryState<?> pending : pendingActivities) {
              if (result.length() > 0) {
                result.append('\n');
              }
              result.append(pending.getStatus());
            }
            return result.toString();
          }
        });
  }

  @Override
  public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
    ActivityRetryState<R> retryState = new ActivityRetryState<R>(input);
    pendingActivities.add(retryState);
    return retryState.execute();
  }
}
