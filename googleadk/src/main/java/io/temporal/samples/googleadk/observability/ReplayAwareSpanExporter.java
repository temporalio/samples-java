package io.temporal.samples.googleadk.observability;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import java.util.Collection;

public final class ReplayAwareSpanExporter implements SpanExporter {
  private final SpanExporter delegate;

  public ReplayAwareSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
  }

  // @@@SNIPSTART java-google-adk-observability-exporter
  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (WorkflowUnsafe.isWorkflowThread() && WorkflowUnsafe.isReplaying()) {
      spans = spans.stream().filter(span -> span.getName().startsWith("HandleQuery:")).toList();
    }
    return spans.isEmpty() ? CompletableResultCode.ofSuccess() : delegate.export(spans);
  }

  // @@@SNIPEND

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }
}
