package io.temporal.samples.nexussecurecredentialpropagation.caller;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.interceptors.Header;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexussecurecredentialpropagation.common.ArgsDigest;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.SecureCredentialHeaders;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Caller Worker side of the credential protocol.
 *
 * <ul>
 *   <li>Opens the start or continue-as-new credential header and keeps the credential in the
 *       per-Workflow interceptor instance. Workflow code never sees it.
 *   <li>Re-seals the credential for the next run on continue-as-new.
 *   <li>Never propagates it to child Workflows.
 *   <li>Seals it to the Nexus handler for protected Nexus Operations, bound to the digest of the
 *       exact input ciphertext.
 * </ul>
 *
 * <p>Register this interceptor last in {@code WorkerFactoryOptions.setWorkerInterceptors}. That
 * makes it the outermost inbound interceptor, so it takes the argument digest immediately after the
 * SDK decodes the arguments, and the innermost outbound interceptor, so the header map it seals
 * into is the one the SDK reads after encoding.
 */
public class CredentialCallerWorkerInterceptor extends WorkerInterceptorBase {
  public static final String CREDENTIAL_REJECTED = "CredentialRejected";

  private static final Logger log = Workflow.getLogger(CredentialCallerWorkerInterceptor.class);

  private final HybridDecrypt workflowCredentialDecrypt;
  private final HybridEncrypt workflowCredentialEncrypt;
  private final HybridEncrypt nexusCredentialEncrypt;
  private final Set<String> protectedRoutes;

  /**
   * @param protectedRoutes Nexus routes, as {@code service/operation}, that receive the credential
   */
  public CredentialCallerWorkerInterceptor(
      HybridDecrypt workflowCredentialDecrypt,
      HybridEncrypt workflowCredentialEncrypt,
      HybridEncrypt nexusCredentialEncrypt,
      Set<String> protectedRoutes) {
    this.workflowCredentialDecrypt = workflowCredentialDecrypt;
    this.workflowCredentialEncrypt = workflowCredentialEncrypt;
    this.nexusCredentialEncrypt = nexusCredentialEncrypt;
    this.protectedRoutes = Collections.unmodifiableSet(new HashSet<>(protectedRoutes));
  }

  public static String route(String service, String operation) {
    return service + "/" + operation;
  }

  @Override
  public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
    return new Inbound(next);
  }

  private final class Inbound extends WorkflowInboundCallsInterceptorBase {
    private CredentialEnvelope credential;

    Inbound(WorkflowInboundCallsInterceptor next) {
      super(next);
    }

    @Override
    public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
      super.init(new Outbound(outboundCalls, this));
    }

    @Override
    public WorkflowOutput execute(WorkflowInput input) {
      // Take the digest first: the SDK decoded the arguments immediately before this call.
      ArgsDigest.Received received = ArgsDigest.take();
      Payload header = input.getHeader().getValues().get(SecureCredentialHeaders.WORKFLOW_HEADER);
      if (header != null) {
        try {
          Object[] args = input.getArguments();
          credential = open(header, received, args == null ? 0 : args.length);
        } catch (GeneralSecurityException e) {
          // Fail the run rather than its Workflow task, so a rejected start ends deterministically.
          // The message is generic; details stay in the Worker log.
          log.warn("Rejected end-user credential: {}", e.getMessage());
          throw ApplicationFailure.newNonRetryableFailure(
              "End-user credential rejected", CREDENTIAL_REJECTED);
        }
      }
      return super.execute(input);
    }

    private CredentialEnvelope open(Payload header, ArgsDigest.Received received, int argCount)
        throws GeneralSecurityException {
      SecureCredentialHeaders.WorkflowHeader parsed =
          SecureCredentialHeaders.parseWorkflowHeader(header);
      WorkflowInfo info = Workflow.getInfo();
      byte[] context;
      if (parsed.getKind() == SecureCredentialHeaders.WorkflowHeaderKind.START) {
        if (received == null || argCount == 0) {
          throw new GeneralSecurityException("No arguments to bind the credential to");
        }
        if (received.getCount() != argCount) {
          // The SDK fills in missing arguments with defaults, so compare against what was sent.
          throw new GeneralSecurityException("Argument count mismatch");
        }
        context =
            SecureCredentialHeaders.startContext(
                info.getNamespace(),
                info.getWorkflowType(),
                info.getWorkflowId(),
                received.getDigest());
      } else {
        // Only runs in the original chain share its first execution run ID; a fresh start that
        // copies this header gets a new one.
        context =
            SecureCredentialHeaders.continueAsNewContext(
                info.getNamespace(),
                info.getWorkflowType(),
                info.getWorkflowId(),
                info.getFirstExecutionRunId());
      }
      return SecureCredentialHeaders.open(
          workflowCredentialDecrypt, parsed.getCiphertext(), context);
    }
  }

  private final class Outbound extends WorkflowOutboundCallsInterceptorBase {
    private final Inbound inbound;

    Outbound(WorkflowOutboundCallsInterceptor next, Inbound inbound) {
      super(next);
      this.inbound = inbound;
    }

    @Override
    public <R> ExecuteNexusOperationOutput<R> executeNexusOperation(
        ExecuteNexusOperationInput<R> input) {
      // The SDK copies the headers into a mutable map when the input is constructed and reads that
      // same map after encoding the operation input.
      Map<String, String> headers = input.getHeaders();
      if (headers.containsKey(SecureCredentialHeaders.NEXUS_HEADER)) {
        throw new IllegalArgumentException("The credential header name is reserved");
      }
      if (!protectedRoutes.contains(route(input.getService(), input.getOperation()))) {
        return super.executeNexusOperation(input);
      }
      if (inbound.credential == null) {
        throw ApplicationFailure.newNonRetryableFailure(
            "No end-user credential for a protected Nexus Operation", CREDENTIAL_REJECTED);
      }
      if (input.getArg() == null) {
        throw new IllegalArgumentException("Protected Nexus Operations require an input");
      }
      NexusSeal seal = new NexusSeal(input, inbound.credential);
      ArgsDigest.arm(seal);
      ExecuteNexusOperationOutput<R> output;
      try {
        output = super.executeNexusOperation(input);
      } finally {
        ArgsDigest.disarm();
      }
      if (!seal.isCompleted()) {
        // Fails the Workflow task, so the command is never sent without a bound credential.
        throw new IllegalStateException(
            "The credential header was not bound to the Nexus operation input");
      }
      return output;
    }

    @Override
    public void continueAsNew(ContinueAsNewInput input) {
      Map<String, Payload> values = withoutCredential(input.getHeader());
      if (inbound.credential != null) {
        WorkflowInfo info = Workflow.getInfo();
        String workflowType =
            input.getWorkflowType() != null ? input.getWorkflowType() : info.getWorkflowType();
        byte[] context =
            SecureCredentialHeaders.continueAsNewContext(
                info.getNamespace(),
                workflowType,
                info.getWorkflowId(),
                info.getFirstExecutionRunId());
        values.put(
            SecureCredentialHeaders.WORKFLOW_HEADER,
            SecureCredentialHeaders.toWorkflowHeaderPayload(
                SecureCredentialHeaders.WorkflowHeaderKind.CONTINUE_AS_NEW,
                SecureCredentialHeaders.seal(
                    workflowCredentialEncrypt, inbound.credential, context)));
      }
      super.continueAsNew(
          new ContinueAsNewInput(
              input.getWorkflowType(), input.getOptions(), input.getArgs(), new Header(values)));
    }

    @Override
    public <R> ChildWorkflowOutput<R> executeChildWorkflow(ChildWorkflowInput<R> input) {
      // Child Workflows never receive the credential.
      return super.executeChildWorkflow(
          new ChildWorkflowInput<>(
              input.getWorkflowId(),
              input.getWorkflowType(),
              input.getResultClass(),
              input.getResultType(),
              input.getArgs(),
              input.getOptions(),
              new Header(withoutCredential(input.getHeader()))));
    }
  }

  private static Map<String, Payload> withoutCredential(Header header) {
    Map<String, Payload> values = new HashMap<>(header.getValues());
    values.remove(SecureCredentialHeaders.WORKFLOW_HEADER);
    return values;
  }

  private final class NexusSeal extends ArgsDigest.PendingSeal {
    private final WorkflowOutboundCallsInterceptor.ExecuteNexusOperationInput<?> input;
    private final CredentialEnvelope credential;

    NexusSeal(
        WorkflowOutboundCallsInterceptor.ExecuteNexusOperationInput<?> input,
        CredentialEnvelope credential) {
      this.input = input;
      this.credential = credential;
    }

    @Override
    protected void seal(byte[] digest, int count) {
      byte[] context =
          SecureCredentialHeaders.nexusContext(
              input.getEndpoint(), input.getService(), input.getOperation(), digest);
      input
          .getHeaders()
          .put(
              SecureCredentialHeaders.NEXUS_HEADER,
              SecureCredentialHeaders.toNexusHeaderValue(
                  SecureCredentialHeaders.seal(nexusCredentialEncrypt, credential, context)));
    }
  }
}
