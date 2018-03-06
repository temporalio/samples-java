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

import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainConfiguration;
import com.uber.cadence.RegisterDomainRequest;
import com.uber.cadence.UpdateDomainRequest;
import com.uber.cadence.WorkflowService;
import org.apache.thrift.TException;

import java.io.IOException;

/**
 * Simple example utility to pretty print workflow execution history. 
 * 
 * @author fateev
 */
public class RegisterDomain {
    
    public static void main(String[] args) throws TException, IOException {
        if (args.length == 1 && "help".equals(args[0])) {
            System.err.println("Usage: java " + RegisterDomain.class.getName() + " <domain-name> [<retention-days>]");
            System.exit(1);
        }
        ConfigHelper configHelper = ConfigHelper.createConfig();
        WorkflowService.Iface swfService = configHelper.createWorkflowClient();
        String domain;
        if (args.length == 1) {
            domain = args[0];
        } else {
            domain = configHelper.getDomain();
        }
        RegisterDomainRequest request = new RegisterDomainRequest();
        request.setDescription("Java Samples");
        request.setEmitMetric(false);
        request.setName(domain);
        int retention = 1;
        if (args.length > 1) {
            try {
                retention = Integer.parseInt(args[1]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot parse retention-days: " + args[1], e);
            }
        }
        request.setWorkflowExecutionRetentionPeriodInDays(retention);
        try {
            swfService.RegisterDomain(request);
            System.out.println("Successfully registered domain \"" + domain + "\" with retentionDays=" + retention);
        } catch (DomainAlreadyExistsError e) {
            UpdateDomainRequest update = new UpdateDomainRequest();
            update.setName(domain);
            update.setConfiguration(new DomainConfiguration().setWorkflowExecutionRetentionPeriodInDays(retention));
            swfService.UpdateDomain(update);
            System.out.println("Successfully updated domain \"" + domain + "\" with retentionDays=" + retention);
        }
        System.exit(0);
    }
    
}
