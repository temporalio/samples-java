/*
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
package com.uber.cadence.samples.common;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.RegisterDomainRequest;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import java.io.IOException;
import org.apache.thrift.TException;

/**
 * Simple example utility to pretty print workflow execution history.
 *
 * @author fateev
 */
public class RegisterDomain {

    public static void main(String[] args) throws TException, IOException {
        IWorkflowService cadenceService = new WorkflowServiceTChannel();
        RegisterDomainRequest request = new RegisterDomainRequest();
        request.setDescription("Java Samples");
        request.setEmitMetric(false);
        request.setName(DOMAIN);
        int retentionPeriodInDays = 1;
        request.setWorkflowExecutionRetentionPeriodInDays(retentionPeriodInDays);
        try {
            cadenceService.RegisterDomain(request);
            System.out.println("Successfully registered domain \"" + DOMAIN + "\" with retentionDays=" + retentionPeriodInDays);
        } catch (DomainAlreadyExistsError e) {
            System.out.println("Domain \"" + DOMAIN + "\" is already registered");
        }
        System.exit(0);
    }
}
