package io.temporal.samples.helloworld;
// @@@START java-hello-world-sample-activity

public class HelloWorldActivity implements HelloWorldActivityInterface {

  @Override
  public String composeGreeting(String name) {
    return "Hello " + name + "!";
  }
}
// @@@END
