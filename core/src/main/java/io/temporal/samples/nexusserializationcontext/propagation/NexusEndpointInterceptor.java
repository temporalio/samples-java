package io.temporal.samples.nexusserializationcontext.propagation;

import io.nexusrpc.OperationException;
import io.nexusrpc.handler.OperationContext;
import io.temporal.common.interceptors.NexusOperationInboundCallsInterceptor;
import io.temporal.common.interceptors.NexusOperationInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.nexus.Nexus;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import org.slf4j.MDC;

/** Makes the endpoint available while a Nexus handler starts its backing workflow. */
public final class NexusEndpointInterceptor extends WorkerInterceptorBase {
  @Override
  public NexusOperationInboundCallsInterceptor interceptNexusOperation(
      OperationContext context, NexusOperationInboundCallsInterceptor next) {
    return new NexusOperationInboundCallsInterceptorBase(next) {
      @Override
      public StartOperationOutput startOperation(StartOperationInput input)
          throws OperationException {
        String previousEndpoint = NexusEndpointContextPropagator.currentEndpoint();
        String endpoint = Nexus.getOperationContext().getInfo().getEndpoint();
        try {
          MDC.put(SampleConfig.ENDPOINT_METADATA_KEY, endpoint);
          return super.startOperation(input);
        } finally {
          if (previousEndpoint == null) {
            MDC.remove(SampleConfig.ENDPOINT_METADATA_KEY);
          } else {
            MDC.put(SampleConfig.ENDPOINT_METADATA_KEY, previousEndpoint);
          }
        }
      }
    };
  }
}
