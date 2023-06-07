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

package io.temporal.samples.payloadconverter;

import static org.junit.Assert.*;

import com.codingrodent.jackson.crypto.CryptoModule;
import com.codingrodent.jackson.crypto.EncryptionService;
import com.codingrodent.jackson.crypto.PasswordCryptoContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.samples.payloadconverter.crypto.CryptoWorkflow;
import io.temporal.samples.payloadconverter.crypto.CryptoWorkflowImpl;
import io.temporal.samples.payloadconverter.crypto.MyCustomer;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class CryptoPayloadConverterTest {
  private static final String encryptDecryptPassword = "encryptDecryptPassword";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowClientOptions(
              WorkflowClientOptions.newBuilder()
                  .setDataConverter(
                      DefaultDataConverter.newDefaultInstance()
                          .withPayloadConverterOverrides(getCryptoJacksonJsonPayloadConverter()))
                  .build())
          .setWorkflowTypes(CryptoWorkflowImpl.class)
          .build();

  @Test
  public void testEncryptedWorkflowData() {
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    CryptoWorkflow workflow =
        testWorkflowRule.getWorkflowClient().newWorkflowStub(CryptoWorkflow.class, workflowOptions);

    MyCustomer customer = workflow.exec(new MyCustomer("John", 22));
    assertNotNull(customer);
    assertTrue(customer.isApproved());
  }

  private JacksonJsonPayloadConverter getCryptoJacksonJsonPayloadConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    // Create the Crypto Context (password based)
    PasswordCryptoContext cryptoContext =
        new PasswordCryptoContext(
            encryptDecryptPassword, // decrypt password
            encryptDecryptPassword, // encrypt password
            PasswordCryptoContext.CIPHER_NAME, // cipher name
            PasswordCryptoContext.KEY_NAME); // key generator names
    EncryptionService encryptionService = new EncryptionService(objectMapper, cryptoContext);
    objectMapper.registerModule(new CryptoModule().addEncryptionService(encryptionService));

    return new JacksonJsonPayloadConverter(objectMapper);
  }
}
