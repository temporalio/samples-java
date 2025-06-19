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
import io.temporal.failure.ApplicationErrorCategory;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.TemporalFailure;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks if the activity method has the {@link BenignExceptionTypes} annotation. If it does, it
 * will throw an ApplicationFailure with {@link ApplicationErrorCategory#BENIGN}.
 */
public class BenignExceptionTypesAnnotationInterceptor extends WorkerInterceptorBase {

  @Override
  public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
    return new ActivityInboundCallsInterceptorAnnotation(next);
  }

  public static class ActivityInboundCallsInterceptorAnnotation
      extends io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase {
    private final ActivityInboundCallsInterceptor next;
    private Set<Class<? extends Exception>> benignExceptionTypes = new HashSet<>();

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
      // Get the @BenignExceptionTypes annotations from the implementation method
      BenignExceptionTypes an = implementationMethod.getAnnotation(BenignExceptionTypes.class);
      if (an != null && an.value() != null) {
        benignExceptionTypes = new HashSet<>(Arrays.asList(an.value()));
      }
      next.init(context);
    }

    @Override
    public ActivityOutput execute(ActivityInput input) {
      if (benignExceptionTypes.isEmpty()) {
        return next.execute(input);
      }
      try {
        return next.execute(input);
      } catch (TemporalFailure tf) {
        throw tf;
      } catch (Exception e) {
        if (benignExceptionTypes.contains(e.getClass())) {
          // If the exception is in the list of benign exceptions, throw an ApplicationFailure
          // with a BENIGN category
          throw ApplicationFailure.newBuilder()
              .setMessage(e.getMessage())
              .setType(e.getClass().getName())
              .setCause(e)
              .setCategory(ApplicationErrorCategory.BENIGN)
              .build();
        }
        // If the exception is not in the list of benign exceptions, rethrow it
        throw e;
      }
    }
  }
}
