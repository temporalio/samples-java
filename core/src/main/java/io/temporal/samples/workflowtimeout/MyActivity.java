package io.temporal.samples.workflowtimeout;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MyActivity {

  @ActivityMethod
  int myActivityMethod();
}
