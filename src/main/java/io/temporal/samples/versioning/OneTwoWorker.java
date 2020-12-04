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

package io.temporal.samples.versioning;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands to run the sample: tctl wf run --tq OneTwoTq --wt OneTwoWorkflow --wid 1 --et 259200
 * tctl wf signal --wid 1 --name "signal" Expected result: workflow returns 1
 *
 * <p>tctl wf run --tq OneTwoTq --wt OneTwoWorkflow --wid 1 --et 259200 Stop the worker and modify
 * OneTwoWorkflowImpl to call activities.two() instead of activities.one(), start the worker. tctl
 * wf signal --wid 1 --name "signal" Expected result: workflow task fails due to non-deterministic
 * error Change two() back to one() and restart the worker. Expected result: workflow finishes
 * gracefully.
 *
 * <p>tctl wf run --tq OneTwoTq --wt OneTwoWorkflow --wid 1 --et 259200 Stop the worker and modify
 * OneTwoWorkflowImpl to call activities.two() instead of activities.one(), start the worker. tctl
 * wf signal --wid 1 --name "signal" Expected result: workflow task fails due to non-deterministic
 * error tctl wf reset --wid 1 --reason "demo" --reset_type FirstWorkflowTask Expected result:
 * workflow returns 2
 *
 * <p>Using versioning to handle non-deterministic changes: tctl wf run --tq OneTwoTq --wt
 * OneTwoWorkflow --wid 1 --et 259200 Stop the worker and replace OneTwoWorkflowImpl.class to
 * OneTwoVersionedWorkflowImpl.class in the registerWorkflowImplementationTypes method call, start
 * the worker. tctl wf signal --wid 1 --name "signal" Expected result: workflow returns 1 tctl wf
 * run --tq OneTwoTq --wt OneTwoWorkflow --wid 1 --et 259200 tctl wf signal --wid 1 --name "signal"
 * Expected result: workflow returns 2
 */
public class OneTwoWorker {

  static final String TASK_QUEUE = "OneTwoTq";

  private static final Logger logger = LoggerFactory.getLogger(OneTwoWorker.class);

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    final Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(OneTwoWorkflowImpl.class);
    // TODO uncomment to test other scenarios
    //    worker.registerWorkflowImplementationTypes(OneTwoVersionedWorkflowImpl.class);
    //    worker.registerWorkflowImplementationTypes(OneTwoWorkflowWithActivityRetry.class);
    //    worker.registerWorkflowImplementationTypes(OneTwoSleepyWorkflowImpl.class);
    worker.registerActivitiesImplementations(new OneTwoActivitiesImpl());
    factory.start();
    logger.info("Worker started for task queue: " + TASK_QUEUE);
  }
}
