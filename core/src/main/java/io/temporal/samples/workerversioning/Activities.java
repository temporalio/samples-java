package io.temporal.samples.workerversioning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface Activities {

  @ActivityMethod
  String someActivity(String calledBy);

  @ActivityMethod
  String someIncompatibleActivity(IncompatibleActivityInput input);

  class IncompatibleActivityInput {
    private final String calledBy;
    private final String moreData;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public IncompatibleActivityInput(
        @JsonProperty("calledBy") String calledBy, @JsonProperty("moreData") String moreData) {
      this.calledBy = calledBy;
      this.moreData = moreData;
    }

    @JsonProperty("calledBy")
    public String getCalledBy() {
      return calledBy;
    }

    @JsonProperty("moreData")
    public String getMoreData() {
      return moreData;
    }
  }
}
