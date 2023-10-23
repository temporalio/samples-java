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

package io.temporal.samples.tracing;

import static org.junit.Assert.*;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sampler;
import io.opentracing.Tracer;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.opentracing.OpenTracingSpanContextCodec;
import io.temporal.opentracing.OpenTracingWorkerInterceptor;
import io.temporal.samples.tracing.workflow.TracingActivitiesImpl;
import io.temporal.samples.tracing.workflow.TracingChildWorkflowImpl;
import io.temporal.samples.tracing.workflow.TracingWorkflow;
import io.temporal.samples.tracing.workflow.TracingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class TracingTest {
  private final InMemoryReporter reporter = new InMemoryReporter();
  private final Sampler sampler = new ConstSampler(true);
  private final Tracer tracer =
      new JaegerTracer.Builder("temporal-test").withReporter(reporter).withSampler(sampler).build();

  private final OpenTracingOptions JAEGER_COMPATIBLE_CONFIG =
      OpenTracingOptions.newBuilder()
          .setSpanContextCodec(OpenTracingSpanContextCodec.TEXT_MAP_CODEC)
          .setTracer(tracer)
          .build();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowClientOptions(
              WorkflowClientOptions.newBuilder()
                  .setInterceptors(new OpenTracingClientInterceptor(JAEGER_COMPATIBLE_CONFIG))
                  .validateAndBuildWithDefaults())
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(new OpenTracingWorkerInterceptor(JAEGER_COMPATIBLE_CONFIG))
                  .validateAndBuildWithDefaults())
          .setWorkflowTypes(TracingWorkflowImpl.class, TracingChildWorkflowImpl.class)
          .setActivityImplementations(new TracingActivitiesImpl())
          .build();

  @After
  public void tearDown() {
    reporter.close();
    sampler.close();
    tracer.close();
  }

  @Test
  public void testReportSpans() {
    WorkflowClient client = testWorkflowRule.getWorkflowClient();
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    TracingWorkflow workflow = client.newWorkflowStub(TracingWorkflow.class, workflowOptions);

    // Convert to untyped and start it with signalWithStart
    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    untyped.signalWithStart("setLanguage", new Object[] {"Spanish"}, new Object[] {"John"});

    String greeting = untyped.getResult(String.class);
    assertEquals("Hola John", greeting);

    List<JaegerSpan> reportedSpans = reporter.getSpans();
    assertNotNull(reportedSpans);
    assertEquals(7, reportedSpans.size());
  }
}
