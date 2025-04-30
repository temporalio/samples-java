package io.temporal.samples.customannotation;

import java.lang.annotation.*;

/** NextRetryDelays is a container annotation for multiple {@link NextRetryDelay} annotations. */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NextRetryDelays {
  NextRetryDelay[] value();
}
