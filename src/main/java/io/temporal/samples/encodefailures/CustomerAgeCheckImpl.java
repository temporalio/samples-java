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

package io.temporal.samples.encodefailures;

import io.temporal.workflow.Workflow;

public class CustomerAgeCheckImpl implements CustomerAgeCheck {
  @Override
  public String validateCustomer(MyCustomer customer) {
    // Note we have explicitly set InvalidCustomerException type to fail workflow execution
    // We wrap it using Workflow.wrap so can throw as unchecked
    if (customer.getAge() < 21) {
      throw Workflow.wrap(
          new InvalidCustomerException("customer " + customer.getName() + " is under age."));
    } else {
      return "done...";
    }
  }
}
