package io.temporal.samples.nexussecurecredentialpropagation.caller;

import com.google.crypto.tink.HybridEncrypt;
import io.temporal.common.interceptors.Header;
import io.temporal.common.interceptors.WorkflowClientCallsInterceptor;
import io.temporal.common.interceptors.WorkflowClientCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowClientInterceptorBase;
import io.temporal.samples.nexussecurecredentialpropagation.common.ArgsDigest;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.SecureCredentialHeaders;
import java.util.function.Supplier;

/**
 * Attaches the credential in {@link CredentialContext} to every Workflow start path: start,
 * signal-with-start, and update-with-start.
 *
 * <p>The header is sealed to the caller Worker's public key and bound to the digest of the exact
 * start-argument ciphertexts. Register this interceptor first in {@code
 * WorkflowClientOptions.setInterceptors} so it is the innermost one: the header map it passes on
 * must be the one the SDK reads after encoding the arguments.
 */
public class CredentialWorkflowClientInterceptor extends WorkflowClientInterceptorBase {
  private final String namespace;
  private final HybridEncrypt workflowCredentialEncrypt;

  public CredentialWorkflowClientInterceptor(
      String namespace, HybridEncrypt workflowCredentialEncrypt) {
    this.namespace = namespace;
    this.workflowCredentialEncrypt = workflowCredentialEncrypt;
  }

  @Override
  public WorkflowClientCallsInterceptor workflowClientCallsInterceptor(
      WorkflowClientCallsInterceptor next) {
    return new WorkflowClientCallsInterceptorBase(next) {
      @Override
      public WorkflowStartOutput start(WorkflowStartInput input) {
        CredentialEnvelope credential = CredentialContext.current();
        if (credential == null) {
          return super.start(rejectReservedHeader(input));
        }
        StartSeal seal = newSeal(input, credential);
        return sealed(seal, () -> super.start(seal.input));
      }

      @Override
      public WorkflowSignalWithStartOutput signalWithStart(WorkflowSignalWithStartInput input) {
        CredentialEnvelope credential = CredentialContext.current();
        if (credential == null) {
          rejectReservedHeader(input.getWorkflowStartInput());
          return super.signalWithStart(input);
        }
        StartSeal seal = newSeal(input.getWorkflowStartInput(), credential);
        return sealed(
            seal,
            () ->
                super.signalWithStart(
                    new WorkflowSignalWithStartInput(
                        seal.input, input.getSignalName(), input.getSignalArguments())));
      }

      @Override
      public <R> WorkflowUpdateWithStartOutput<R> updateWithStart(
          WorkflowUpdateWithStartInput<R> input) {
        CredentialEnvelope credential = CredentialContext.current();
        if (credential == null) {
          rejectReservedHeader(input.getWorkflowStartInput());
          return super.updateWithStart(input);
        }
        StartSeal seal = newSeal(input.getWorkflowStartInput(), credential);
        return sealed(
            seal,
            () ->
                super.updateWithStart(
                    new WorkflowUpdateWithStartInput<>(seal.input, input.getStartUpdateInput())));
      }
    };
  }

  private StartSeal newSeal(
      WorkflowClientCallsInterceptor.WorkflowStartInput input, CredentialEnvelope credential) {
    if (input.getArguments() == null || input.getArguments().length == 0) {
      // With no arguments there is nothing to bind the credential to.
      throw new IllegalArgumentException(
          "Workflows started with an end-user credential must take at least one argument");
    }
    rejectReservedHeader(input);
    ReadTrackingHeaderMap values = new ReadTrackingHeaderMap(input.getHeader().getValues());
    WorkflowClientCallsInterceptor.WorkflowStartInput withHeader =
        new WorkflowClientCallsInterceptor.WorkflowStartInput(
            input.getWorkflowId(),
            input.getWorkflowType(),
            new Header(values),
            input.getArguments(),
            input.getOptions());
    return new StartSeal(withHeader, values, credential);
  }

  private static <T> T sealed(StartSeal seal, Supplier<T> call) {
    ArgsDigest.arm(seal);
    T result;
    try {
      result = call.get();
    } finally {
      ArgsDigest.disarm();
    }
    if (!seal.isCompleted() || !seal.headerValues.wasReadAfterSeal()) {
      // The SDK did not send the sealed header, for example because an inner interceptor copied
      // the header map before the arguments were encoded. The Workflow starts without a
      // credential and its protected calls fail; surface the misconfiguration here.
      throw new IllegalStateException("The credential header was not bound to the start arguments");
    }
    return result;
  }

  private static WorkflowClientCallsInterceptor.WorkflowStartInput rejectReservedHeader(
      WorkflowClientCallsInterceptor.WorkflowStartInput input) {
    if (input.getHeader().getValues().containsKey(SecureCredentialHeaders.WORKFLOW_HEADER)) {
      throw new IllegalArgumentException("The credential header name is reserved");
    }
    return input;
  }

  private final class StartSeal extends ArgsDigest.PendingSeal {
    private final WorkflowClientCallsInterceptor.WorkflowStartInput input;
    private final ReadTrackingHeaderMap headerValues;
    private final CredentialEnvelope credential;

    StartSeal(
        WorkflowClientCallsInterceptor.WorkflowStartInput input,
        ReadTrackingHeaderMap headerValues,
        CredentialEnvelope credential) {
      this.input = input;
      this.headerValues = headerValues;
      this.credential = credential;
    }

    @Override
    protected void seal(byte[] digest, int count) {
      if (count != input.getArguments().length) {
        throw new IllegalStateException("Encoded argument count does not match the start input");
      }
      byte[] context =
          SecureCredentialHeaders.startContext(
              namespace, input.getWorkflowType(), input.getWorkflowId(), digest);
      headerValues.put(
          SecureCredentialHeaders.WORKFLOW_HEADER,
          SecureCredentialHeaders.toWorkflowHeaderPayload(
              SecureCredentialHeaders.WorkflowHeaderKind.START,
              SecureCredentialHeaders.seal(workflowCredentialEncrypt, credential, context)));
      headerValues.markSealed();
    }
  }
}
