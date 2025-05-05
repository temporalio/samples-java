

package io.temporal.samples.countinterceptor.activities;

public class MyActivitiesImpl implements MyActivities {
  @Override
  public String sayHello(String name, String title) {
    return "Hello " + title + " " + name;
  }

  @Override
  public String sayGoodBye(String name, String title) {
    return "Goodbye  " + title + " " + name;
  }
}
