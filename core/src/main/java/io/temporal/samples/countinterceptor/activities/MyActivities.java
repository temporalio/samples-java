

package io.temporal.samples.countinterceptor.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface MyActivities {
  String sayHello(String name, String title);

  String sayGoodBye(String name, String title);
}
