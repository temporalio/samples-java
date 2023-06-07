/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

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
