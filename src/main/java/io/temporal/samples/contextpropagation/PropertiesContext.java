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

package io.temporal.samples.contextpropagation;

import com.google.common.io.Resources;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Properties;

public class PropertiesContext {

  private static final Properties appProps = new Properties();

  static {
    String appPath = Resources.getResource("app.properties").getPath();
    try {
      appProps.load(new FileInputStream(appPath));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void put(String key, String val) throws IllegalArgumentException {
    if (key == null) {
      throw new IllegalArgumentException("key parameter cannot be null");
    }
    appProps.put(key, val);
  }

  public static String get(String key) throws IllegalArgumentException {
    if (key == null) {
      throw new IllegalArgumentException("key parameter cannot be null");
    }

    return appProps.getProperty(key);
  }

  public static void remove(String key) throws IllegalArgumentException {
    if (key == null) {
      throw new IllegalArgumentException("key parameter cannot be null");
    }

    appProps.remove(key);
  }

  public static void clear() {
    appProps.clear();
  }

  public static void list(PrintStream out) {
    appProps.list(out);
  }

  public static Properties getAppPropsCopy() {
    return (Properties) appProps.clone();
  }

  public static Properties getProperties() {
    return appProps;
  }
}
