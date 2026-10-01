package io.temporal.samples.nexusserializationcontext.handler;

public final class EchoWorkflowImpl implements EchoWorkflow {
  @Override
  public String echo(String message) {
    return message;
  }
}
