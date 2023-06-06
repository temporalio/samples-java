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

package io.temporal.samples.batch.iterator;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Random;
import org.slf4j.Logger;

/** Fake RecordProcessorWorkflow implementation. */
public class RecordProcessorWorkflowImpl implements RecordProcessorWorkflow {
  public static final Logger log = Workflow.getLogger(RecordProcessorWorkflowImpl.class);
  private final Random random = Workflow.newRandom();

  @Override
  public void processRecord(SingleRecord r) {
    // Simulate some processing
    Workflow.sleep(Duration.ofSeconds(random.nextInt(30)));
    log.info("Processed " + r);
  }
}
