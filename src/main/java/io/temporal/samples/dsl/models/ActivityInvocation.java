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
  public String[] arguments;
  public String result;

  @JsonCreator
  public ActivityInvocation(
      @JsonProperty("name") String name,
      @JsonProperty("arguments") String[] arguments,
      @JsonProperty("result") String result) {
    this.name = name;
    this.arguments = arguments;
    this.result = result;
  }

  public Void execute(Map<String, String> bindings) {
    String[] args = makeInput(this.arguments, bindings);
    // Class.forName(name);

    ActivityStub stub =
        Workflow.newUntypedActivityStub(
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(1))
                .setTaskQueue("dsl")
                .build());

    // Warning activity task takes in non-varargs call of varargs method with inexact argument type
    // for last parameter;
    // String results = stub.execute(name, String.class, args);
    //                                                   ^
    // cast to Object for a varargs call
    // cast to Object[] for a non-varargs call and to suppress this warning
    String results = stub.execute(name, String.class, new Object[] {args});

    if (Strings.isNullOrEmpty(this.result)) {
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
