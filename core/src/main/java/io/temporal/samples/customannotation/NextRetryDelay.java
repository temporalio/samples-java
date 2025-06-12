package io.temporal.samples.customannotation;

import java.lang.annotation.*;

/**
 * NextRetryDelay is an annotation that can be used to specify the next retry delay for a particular
 * failure type in a Temporal activity. It is used to provide a custom fixed delay if the activity
 * fails with a specific exception type.
 *
 * <p>For this annotation to work, {@link NextRetryDelayActivityAnnotationInterceptor} must be
 * passed as a worker interceptor to the worker factory.
 */
@Documented
@Target(ElementType.METHOD)
@Repeatable(NextRetryDelays.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface NextRetryDelay {
  /**
   * failureType is the type of failure that this retry delay applies to. It should be the fully
   * qualified class name of the exception type or the type of the {@link
   * io.temporal.failure.ApplicationFailure}.
   */
  String failureType();

  /**
   * delaySeconds is the fixed delay in seconds that should be applied for the specified failure
   * type.
   */
  int delaySeconds();
}
