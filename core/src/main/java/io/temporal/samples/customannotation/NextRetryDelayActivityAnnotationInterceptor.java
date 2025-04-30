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

package io.temporal.samples.customannotation;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.common.metadata.POJOActivityImplMetadata;
import io.temporal.common.metadata.POJOActivityMethodMetadata;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.TemporalFailure;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks if the activity method has the @NextRetryDelay annotation. If it does, it will throw an
 * ApplicationFailure with a delay set to the value of the annotation.
 */
public class NextRetryDelayActivityAnnotationInterceptor extends WorkerInterceptorBase {

  @Override
  public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
    return new ActivityInboundCallsInterceptorAnnotation(next);
  }

  public static class ActivityInboundCallsInterceptorAnnotation
      extends io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase {
    private final ActivityInboundCallsInterceptor next;
    private Map<String, Integer> delaysPerType = new HashMap<>();

    public ActivityInboundCallsInterceptorAnnotation(ActivityInboundCallsInterceptor next) {
      super(next);
      this.next = next;
    }

    @Override
    public void init(ActivityExecutionContext context) {
      List<POJOActivityMethodMetadata> activityMethods =
          POJOActivityImplMetadata.newInstance(context.getInstance().getClass())
              .getActivityMethods();
      // TODO: handle dynamic activity types
      POJOActivityMethodMetadata currentActivityMethod =
          activityMethods.stream()
              .filter(x -> x.getActivityTypeName().equals(context.getInfo().getActivityType()))
              .findFirst()
              .get();
      // Get the implementation method from the interface method
      Method implementationMethod;
      try {
        implementationMethod =
            context
                .getInstance()
                .getClass()
                .getMethod(
                    currentActivityMethod.getMethod().getName(),
                    currentActivityMethod.getMethod().getParameterTypes());
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
      // Get the @NextRetryDelay annotations from the implementation method
      NextRetryDelay[] an = implementationMethod.getAnnotationsByType(NextRetryDelay.class);
      for (NextRetryDelay a : an) {
        delaysPerType.put(a.failureType(), a.delaySeconds());
      }
      next.init(context);
    }

    @Override
    public ActivityOutput execute(ActivityInput input) {
      if (delaysPerType.size() == 0) {
        return next.execute(input);
      }
      try {
        return next.execute(input);
      } catch (ApplicationFailure ae) {
        Integer delay = delaysPerType.get(ae.getType());
        if (delay != null) {
          // TODO: make sure to pass all the other parameters to the new ApplicationFailure
          throw ApplicationFailure.newFailureWithCauseAndDelay(
              ae.getMessage(), ae.getType(), ae.getCause(), Duration.ofSeconds(delay));
        }
        throw ae;
      } catch (TemporalFailure tf) {
        throw tf;
      } catch (Exception e) {
        Integer delay = delaysPerType.get(e.getClass().getName());
        if (delay != null) {
          throw ApplicationFailure.newFailureWithCauseAndDelay(
              e.getMessage(), e.getClass().getName(), e, Duration.ofSeconds(delay));
        }
        throw e;
      }
    }
  }
}
