package io.temporal.samples.helloworld;
// @@@SNIPSTART java-hello-world-sample-activity-interface
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface HelloWorldActivity {

  @ActivityMethod
  String composeGreeting(String name);
}
// @@@SNIPEND
