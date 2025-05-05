package io.temporal.samples.countinterceptor.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.countinterceptor.activities.MyActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyChildWorkflowImpl implements MyChildWorkflow {
  @Override
  public String execChild(String name, String title) {
    MyActivities activities =
        Workflow.newActivityStub(
            MyActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    String result = activities.sayHello(name, title);
    result += activities.sayGoodBye(name, title);

    return result;
  }
}
