

package io.temporal.samples.springboot.hello;

import io.temporal.samples.springboot.hello.model.Person;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "HelloSampleTaskQueue")
public class HelloActivityImpl implements HelloActivity {
  @Value("${samples.data.language}")
  private String language;

  @Override
  public String hello(Person person) {
    String greeting = language.equals("spanish") ? "Hola " : "Hello ";
    return greeting + person.getFirstName() + " " + person.getLastName() + "!";
  }
}
