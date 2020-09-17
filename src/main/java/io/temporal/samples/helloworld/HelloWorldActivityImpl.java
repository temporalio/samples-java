package io.temporal.samples.helloworld;
// @@@SNIPSTART java-hello-world-sample-activity
public class HelloWorldActivityImpl implements HelloWorldActivity {

  @Override
  public String composeGreeting(String name) {
    // Append and strings and return the new one
    return "Hello " + name + "!";
  }
}
// @@@SNIPEND
