package io.temporal.samples.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.activity.ActivityInterface;
import io.temporal.testing.TestActivityExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Unit test for {@link HelloDynamic.DynamicGreetingActivityImpl}. Doesn't use an external Temporal
 * service.
 */
public class HelloDynamicActivityJUnit5Test {

  @RegisterExtension
  public static final TestActivityExtension testActivityExtension =
      TestActivityExtension.newBuilder()
          .setActivityImplementations(new HelloDynamic.DynamicGreetingActivityImpl())
          .build();

  /**
   * Dynamic activity {@link HelloDynamic.DynamicGreetingActivityImpl} is injected as an
   * implementation for a static activity interface {@link MyStaticActivity}.
   */
  @Test
  public void testDynamicActivity(MyStaticActivity activity) {
    String result = activity.response("Hello", "John", "HelloDynamicActivityJUnit5Test");

    assertEquals("MyStaticResponse: Hello John from: HelloDynamicActivityJUnit5Test", result);
  }

  @ActivityInterface(namePrefix = "MyStatic")
  public interface MyStaticActivity {
    String response(String name, String greeting, String source);
  }
}
