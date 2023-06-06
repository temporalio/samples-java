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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkflowImplementationOptions;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class EncodeFailuresTest {

  CodecDataConverter codecDataConverter =
      new CodecDataConverter(
          DefaultDataConverter.newDefaultInstance(),
          Collections.singletonList(new SimplePrefixPayloadCodec()),
          true);

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              WorkflowImplementationOptions.newBuilder()
                  .setFailWorkflowExceptionTypes(InvalidCustomerException.class)
                  .build(),
              CustomerAgeCheckImpl.class)
          .setWorkflowClientOptions(
              WorkflowClientOptions.newBuilder().setDataConverter(codecDataConverter).build())
          .build();

  @Test
  public void testFailureMessageIsEncoded() {

    CustomerAgeCheck workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                CustomerAgeCheck.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("CustomerAgeCheck")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    assertThrows(
        WorkflowFailedException.class,
        () -> {
          workflow.validateCustomer(new MyCustomer("John", 20));
        });

    HistoryEvent wfExecFailedEvent =
        testWorkflowRule.getWorkflowClient().fetchHistory("CustomerAgeCheck").getLastEvent();
    Payload payload =
        wfExecFailedEvent
            .getWorkflowExecutionFailedEventAttributes()
            .getFailure()
            .getEncodedAttributes();
    assertTrue(isEncoded(payload));
  }

  private boolean isEncoded(Payload payload) {
    return payload.getData().startsWith(SimplePrefixPayloadCodec.PREFIX);
  }
}
