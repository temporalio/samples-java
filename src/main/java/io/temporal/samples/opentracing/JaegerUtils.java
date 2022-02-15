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

package io.temporal.samples.opentracing;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import io.opentracing.Tracer;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.opentracing.OpenTracingSpanContextCodec;
import org.apache.thrift.transport.TTransportException;

public class JaegerUtils {
  public static OpenTracingOptions getJaegerOptions() {
    try {
      // Using Udp Sender, make sure to change host and port
      // to your Jaeger options (if using different than in sample)
      RemoteReporter reporter =
          new RemoteReporter.Builder().withSender(new UdpSender("localhost", 5775, 0)).build();
      Sampler sampler = new ConstSampler(true);
      Tracer tracer =
          new JaegerTracer.Builder("temporal-sample")
              .withReporter(reporter)
              .withSampler(sampler)
              .build();

      OpenTracingOptions options =
          OpenTracingOptions.newBuilder()
              .setSpanContextCodec(OpenTracingSpanContextCodec.TEXT_MAP_CODEC)
              .setTracer(tracer)
              .build();
      return options;
    } catch (TTransportException e) {
      System.out.println("Exception configuring Jaeger Tracer: " + e.getMessage());
      return null;
    }
  }
}
