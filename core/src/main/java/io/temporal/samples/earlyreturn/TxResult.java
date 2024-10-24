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

package io.temporal.samples.earlyreturn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TxResult {
  private final String transactionId;
  private final String status;

  // Jackson-compatible constructor with @JsonCreator and @JsonProperty annotations
  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public TxResult(
      @JsonProperty("transactionId") String transactionId, @JsonProperty("status") String status) {
    this.transactionId = transactionId;
    this.status = status;
  }

  @JsonProperty("transactionId")
  public String getTransactionId() {
    return transactionId;
  }

  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return String.format("InitResult{transactionId='%s', status='%s'}", transactionId, status);
  }
}
