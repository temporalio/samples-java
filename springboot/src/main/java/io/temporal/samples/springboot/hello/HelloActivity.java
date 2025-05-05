package io.temporal.samples.springboot.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.samples.springboot.hello.model.Person;

@ActivityInterface
public interface HelloActivity {
  String hello(Person person);
}
