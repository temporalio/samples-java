package io.temporal.samples.dsl.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Map;

public class ActivityInvocation {
  public String name;
  public String method;
  public String[] arguments;
  public String result;

  @JsonCreator
  public ActivityInvocation(
      @JsonProperty("name") String name,
      @JsonProperty("method") String method,
      @JsonProperty("arguments") String[] arguments,
      @JsonProperty("result") String result) {
    this.name = name;
    this.arguments = arguments;
    this.result = result;
    this.method = method;
  }

  public Void execute(Map<String, String> bindings) {
    String[] args = makeInput(this.arguments, bindings);
    ActivityStub stub =
        Workflow.newUntypedActivityStub(
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setTaskQueue("dsl")
                .build());

    String results = stub.execute(this.method, String.class, new Object[] {args});

    if (!Strings.isNullOrEmpty(this.result)) {
      bindings.put(this.result, results);
    }

    return null;
  }

  private String[] makeInput(String[] arguments, Map<String, String> argsMap) {
    String[] args = new String[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      args[i] = argsMap.get(arguments[i]);
    }
    return args;
  }
}
