package io.temporal.samples.dsl;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

public class SampleActivities {
  @ActivityInterface(namePrefix = "SampleActivity1")
  public interface SampleActivities1 {
    @ActivityMethod(name = "SampleActivity1.getInfo")
    String getInfo();
  }

  @ActivityInterface(namePrefix = "SampleActivity2")
  public interface SampleActivities2 {
    @ActivityMethod(name = "SampleActivity2.getInfo")
    String getInfo();
  }

  @ActivityInterface(namePrefix = "SampleActivity3")
  public interface SampleActivities3 {
    @ActivityMethod(name = "SampleActivity3.getInfo")
    String getInfo();
  }

  @ActivityInterface(namePrefix = "SampleActivity4")
  public interface SampleActivities4 {
    @ActivityMethod(name = "SampleActivity4.getInfo")
    String getInfo();
  }

  @ActivityInterface(namePrefix = "SampleActivity5")
  public interface SampleActivities5 {
    @ActivityMethod(name = "SampleActivity5.getInfo")
    String getInfo();
  }

  public static class SampleActivitiesImpl1 implements SampleActivities1 {
    @Override
    public String getInfo() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl2 implements SampleActivities2 {
    @Override
    public String getInfo() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl3 implements SampleActivities3 {
    @Override
    public String getInfo() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl4 implements SampleActivities4 {
    @Override
    public String getInfo() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl5 implements SampleActivities5 {
    @Override
    public String getInfo() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }
}
