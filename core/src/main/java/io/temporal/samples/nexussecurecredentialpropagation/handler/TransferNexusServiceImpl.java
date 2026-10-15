package io.temporal.samples.nexussecurecredentialpropagation.handler;

import com.google.crypto.tink.Mac;
import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowHandle;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import io.temporal.samples.nexussecurecredentialpropagation.common.Frame;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ServiceImpl(service = TransferService.class)
public class TransferNexusServiceImpl {
  private final Mac operationTokenMac;

  public TransferNexusServiceImpl(Mac operationTokenMac) {
    this.operationTokenMac = operationTokenMac;
  }

  /**
   * The backing Workflow ID is derived from the verified principal and the business identity of the
   * transfer, never from the Nexus request ID. Including the principal stops a second user with
   * identical input from attaching to the first user's transfer. Hashing keeps user identifiers out
   * of Workflow IDs.
   */
  public static String resourceId(EndUserPrincipal principal, TransferService.TransferInput input) {
    byte[] identity =
        Frame.of("transfer-resource")
            .add(principal.getIssuer())
            .add(principal.getTenant())
            .add(principal.getSubject())
            .add(input.getPaymentId())
            .toByteArray();
    try {
      StringBuilder id = new StringBuilder("transfer-");
      for (byte b : MessageDigest.getInstance("SHA-256").digest(identity)) {
        id.append(String.format("%02x", b));
      }
      return id.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @OperationImpl
  public OperationHandler<TransferService.TransferInput, TransferService.TransferOutput>
      transfer() {
    return new SignedTokenOperationHandler<>(
        operationTokenMac,
        TransferNexusServiceImpl::resourceId,
        (workflowId, principal, input) ->
            WorkflowHandle.fromWorkflowMethod(
                Nexus.getOperationContext()
                        .getWorkflowClient()
                        .newWorkflowStub(
                            TransferHandlerWorkflow.class,
                            WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId)
                                // Attach to the existing transfer instead of failing or running it
                                // again. Uniqueness beyond retention needs an external store.
                                .setWorkflowIdConflictPolicy(
                                    WorkflowIdConflictPolicy
                                        .WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                                .setWorkflowIdReusePolicy(
                                    WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                                .build())
                    ::settle,
                new TransferHandlerWorkflow.Settlement(
                    input.getPaymentId(),
                    input.getBeneficiary(),
                    input.getAmountCents(),
                    principal.getSubject())));
  }
}
