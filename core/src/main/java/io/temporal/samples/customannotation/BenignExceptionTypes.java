package io.temporal.samples.customannotation;

import java.lang.annotation.*;

/**
 * BenignExceptionTypes is an annotation that can be used to specify an exception type is benign and
 * not a issue worth logging.
 *
 * <p>For this annotation to work, {@link BenignExceptionTypesAnnotationInterceptor} must be passed
 * as a worker interceptor to the worker factory.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BenignExceptionTypes {
  /** Type of exceptions that should be considered benign and not logged as errors. */
  Class<? extends Exception>[] value();
}
