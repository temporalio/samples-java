

package io.temporal.samples.springboot.customize;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "CustomizeTaskQueue")
public class CustomizeActivityImpl implements CustomizeActivity {
  @Override
  public String run(String input) {
    return "Completed as " + input + " activity!";
  }
}
