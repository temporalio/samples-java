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

package io.temporal.samples.payloadconverter.crypto;

import com.codingrodent.jackson.crypto.Encrypt;

public class MyCustomer {
  private String name;
  private int age;
  private boolean approved;

  public MyCustomer() {}

  public MyCustomer(String name, int age) {
    this.name = name;
    this.age = age;
  }

  @Encrypt
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Encrypt
  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Encrypt
  public boolean isApproved() {
    return approved;
  }

  public void setApproved(boolean approved) {
    this.approved = approved;
  }
}
