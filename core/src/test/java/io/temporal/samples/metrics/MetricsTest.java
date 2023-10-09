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

package io.temporal.samples.metrics;

import static junit.framework.TestCase.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import com.uber.m3.util.Duration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.samples.metrics.activities.MetricsActivitiesImpl;
import io.temporal.samples.metrics.workflow.MetricsWorkflow;
import io.temporal.samples.metrics.workflow.MetricsWorkflowImpl;
import io.temporal.serviceclient.MetricsTag;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerOptions;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MetricsTest {

  private static final long REPORTING_FLUSH_TIME = 50;
  private static List<Tag> TAGS_NAMESPACE_QUEUE;
  private final String SDK_CUSTOM_KEY = "sdkCustomTag1Key";
  private final String SDK_CUSTOM_VALUE = "sdkCustomTag1Value";
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final StatsReporter reporter = new MicrometerClientStatsReporter(registry);
  private final Scope metricsScope =
      new RootScopeBuilder()
          .tags(ImmutableMap.of(SDK_CUSTOM_KEY, SDK_CUSTOM_VALUE))
          .reporter(reporter)
          .reportEvery(Duration.ofMillis(REPORTING_FLUSH_TIME >> 1));
  private final String TEST_NAMESPACE = "UnitTest";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(MetricsWorkflowImpl.class)
          .setMetricsScope(metricsScope)
          .setWorkerOptions(WorkerOptions.newBuilder().build())
          .setActivityImplementations(new MetricsActivitiesImpl())
          .build();

  private WorkflowServiceStubs clientStubs;
  private WorkflowClient workflowClient;

  private static List<Tag> replaceTags(List<Tag> tags, String... nameValuePairs) {
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      tags = replaceTag(tags, nameValuePairs[i], nameValuePairs[i + 1]);
    }
    return tags;
  }

  private static List<Tag> replaceTag(List<Tag> tags, String name, String value) {
    List<Tag> result =
        tags.stream().filter(tag -> !name.equals(tag.getKey())).collect(Collectors.toList());
    result.add(new ImmutableTag(name, value));
    return result;
  }

  @Before
  public void setUp() {

    final WorkflowServiceStubsOptions options =
        testWorkflowRule.getWorkflowClient().getWorkflowServiceStubs().getOptions();

    this.clientStubs = WorkflowServiceStubs.newServiceStubs(options);

    this.workflowClient =
        WorkflowClient.newInstance(clientStubs, testWorkflowRule.getWorkflowClient().getOptions());

    final Map<String, String> stringStringMap = MetricsTag.defaultTags(TEST_NAMESPACE);
    final List<Tag> TAGS_NAMESPACE =
        stringStringMap.entrySet().stream()
            .map(
                nameValueEntry ->
                    new ImmutableTag(nameValueEntry.getKey(), nameValueEntry.getValue()))
            .collect(Collectors.toList());

    TAGS_NAMESPACE_QUEUE =
        replaceTags(TAGS_NAMESPACE, MetricsTag.TASK_QUEUE, testWorkflowRule.getTaskQueue());
  }

  @After
  public void tearDown() {
    this.clientStubs.shutdownNow();
    this.registry.close();
  }

  @Test
  public void testCountActivityRetriesMetric() throws InterruptedException {
    final MetricsWorkflow metricsWorkflow =
        workflowClient.newWorkflowStub(
            MetricsWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .validateBuildWithDefaults());

    metricsWorkflow.exec("hello metrics");

    Thread.sleep(REPORTING_FLUSH_TIME);

    assertIntCounter(4, countMetricActivityRetriesForActivity("PerformB"));

    assertIntCounter(2, countMetricActivityRetriesForActivity("PerformA"));
  }

  @NotNull
  private Counter countMetricActivityRetriesForActivity(String performB) {
    final List<Tag> tags =
        replaceTags(
            TAGS_NAMESPACE_QUEUE,
            MetricsTag.ACTIVITY_TYPE,
            performB,
            MetricsTag.WORKFLOW_TYPE,
            "MetricsWorkflow",
            MetricsTag.WORKER_TYPE,
            "ActivityWorker",
            SDK_CUSTOM_KEY,
            SDK_CUSTOM_VALUE);
    return registry.counter("custom_activity_retries", tags);
  }

  private void assertIntCounter(int expectedValue, Counter counter) {
    assertEquals(expectedValue, Math.round(counter.count()));
  }
}
