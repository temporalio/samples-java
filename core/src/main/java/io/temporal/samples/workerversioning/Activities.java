package io.temporal.samples.workerversioning;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface Activities {

  @ActivityMethod
  String someActivity(String calledBy);

  @ActivityMethod
  String someIncompatibleActivity(IncompatibleActivityInput input);

  class IncompatibleActivityInput {
    String calledBy;
    String moreData;

    public IncompatibleActivityInput() {}

    public IncompatibleActivityInput(String calledBy, String moreData) {
      this.calledBy = calledBy;
      this.moreData = moreData;
    }

    public String getCalledBy() {
      return calledBy;
    }

    public String getMoreData() {
      return moreData;
    }

    public void setCalledBy(String calledBy) {
      this.calledBy = calledBy;
    }

    public void setMoreData(String moreData) {
      this.moreData = moreData;
    }
  }
}
