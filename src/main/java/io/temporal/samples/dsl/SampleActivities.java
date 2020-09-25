package io.temporal.samples.dsl;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

public class SampleActivities {
  @ActivityInterface
  public interface SampleActivities1 {
    @ActivityMethod
    String getInfo1();
  }

  @ActivityInterface
  public interface SampleActivities2 {
    @ActivityMethod
    String getInfo2();
  }

  @ActivityInterface
  public interface SampleActivities3 {
    @ActivityMethod
    String getInfo3();
  }

  @ActivityInterface
  public interface SampleActivities4 {
    @ActivityMethod
    String getInfo4();
  }

  @ActivityInterface
  public interface SampleActivities5 {
    @ActivityMethod
    String getInfo5();
  }

  public static class SampleActivitiesImpl1 implements SampleActivities1 {
    @Override
    public String getInfo1() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl2 implements SampleActivities2 {
    @Override
    public String getInfo2() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl3 implements SampleActivities3 {
    @Override
    public String getInfo3() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl4 implements SampleActivities4 {
    @Override
    public String getInfo4() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }

  public static class SampleActivitiesImpl5 implements SampleActivities5 {
    @Override
    public String getInfo5() {
      String name = Activity.getExecutionContext().getInfo().getActivityType();
      return "Result_" + name;
    }
  }
}
