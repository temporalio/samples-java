package io.temporal.samples.nexussecurecredentialpropagation;

import io.nexusrpc.handler.HandlerException;
import io.temporal.common.converter.RawValue;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexussecurecredentialpropagation.handler.TransferHandlerWorkflow;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.NexusServiceStub;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Workflows used only by the tests. */
final class TestWorkflows {
  private TestWorkflows() {}

  static TransferService newTransferStub() {
    return Workflow.newNexusServiceStub(
        TransferService.class,
        NexusServiceOptions.newBuilder()
            .setOperationOptions(
                NexusOperationOptions.newBuilder()
                    .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                    .build())
            .build());
  }

  /** Settles immediately, or waits until cancelled when the payment ID starts with "hold-". */
  public static class QuickSettleWorkflowImpl implements TransferHandlerWorkflow {
    @Override
    public TransferService.TransferOutput settle(Settlement settlement) {
      if (settlement.getPaymentId().startsWith("hold-")) {
        Workflow.await(() -> false);
      }
      return new TransferService.TransferOutput(
          settlement.getPaymentId(), "SETTLED", settlement.getApprovedFor());
    }
  }

  @WorkflowInterface
  public interface SteerableTransferWorkflow {
    @WorkflowMethod
    TransferService.TransferOutput transfer(TransferService.TransferInput input);

    @SignalMethod
    void ping(String value);

    @UpdateMethod
    String ack(String value);
  }

  public static class SteerableTransferWorkflowImpl implements SteerableTransferWorkflow {
    private final TransferService transferService = newTransferStub();

    @Override
    public TransferService.TransferOutput transfer(TransferService.TransferInput input) {
      return transferService.transfer(input);
    }

    @Override
    public void ping(String value) {}

    @Override
    public String ack(String value) {
      return value;
    }
  }

  @WorkflowInterface
  public interface TwoArgWorkflow {
    @WorkflowMethod
    String join(String first, String second);
  }

  public static class TwoArgWorkflowImpl implements TwoArgWorkflow {
    @Override
    public String join(String first, String second) {
      return first + "," + second;
    }
  }

  @WorkflowInterface
  public interface NoArgWorkflow {
    @WorkflowMethod
    String run();
  }

  public static class NoArgWorkflowImpl implements NoArgWorkflow {
    @Override
    public String run() {
      return "ok";
    }
  }

  /** Continues as new into {@link FlakyTransferWorkflow}, a different Workflow type. */
  @WorkflowInterface
  public interface ContinuingWorkflow {
    @WorkflowMethod
    TransferService.TransferOutput transfer(TransferService.TransferInput input);
  }

  public static class ContinuingWorkflowImpl implements ContinuingWorkflow {
    @Override
    public TransferService.TransferOutput transfer(TransferService.TransferInput input) {
      return Workflow.newContinueAsNewStub(FlakyTransferWorkflow.class).transfer(input);
    }
  }

  /** Fails its first attempt, so the credential must also survive a retry after continue-as-new. */
  @WorkflowInterface
  public interface FlakyTransferWorkflow {
    @WorkflowMethod
    TransferService.TransferOutput transfer(TransferService.TransferInput input);
  }

  public static class FlakyTransferWorkflowImpl implements FlakyTransferWorkflow {
    private final TransferService transferService = newTransferStub();

    @Override
    public TransferService.TransferOutput transfer(TransferService.TransferInput input) {
      if (Workflow.getInfo().getAttempt() == 1) {
        throw ApplicationFailure.newFailure("first attempt fails", "Flaky");
      }
      return transferService.transfer(input);
    }
  }

  @WorkflowInterface
  public interface ParentWorkflow {
    @WorkflowMethod
    String run(String childId);
  }

  public static class ParentWorkflowImpl implements ParentWorkflow {
    @Override
    public String run(String childId) {
      return Workflow.newChildWorkflowStub(
              NoArgWorkflow.class, ChildWorkflowOptions.newBuilder().setWorkflowId(childId).build())
          .run();
    }
  }

  @WorkflowInterface
  public interface CancellingTransferWorkflow {
    @WorkflowMethod
    String transferThenCancel(TransferService.TransferInput input);
  }

  public static class CancellingTransferWorkflowImpl implements CancellingTransferWorkflow {
    private final TransferService transferService = newTransferStub();

    @Override
    public String transferThenCancel(TransferService.TransferInput input) {
      NexusOperationHandle<TransferService.TransferOutput>[] handle = new NexusOperationHandle[1];
      CancellationScope scope =
          Workflow.newCancellationScope(
              () -> handle[0] = Workflow.startNexusOperation(transferService::transfer, input));
      scope.run();
      String token = handle[0].getExecution().get().getOperationToken().orElse("");
      scope.cancel();
      try {
        handle[0].getResult().get();
        return "completed";
      } catch (RuntimeException e) {
        return "cancelled:" + token;
      }
    }
  }

  /**
   * A caller with no keys that replays captured Nexus input bytes and headers. It uses a data
   * converter without the codec, so the captured ciphertext is sent unchanged.
   */
  @WorkflowInterface
  public interface AttackerWorkflow {
    @WorkflowMethod
    String replay(String endpoint, byte[] serializedInput);
  }

  public static class AttackerWorkflowImpl implements AttackerWorkflow {
    @Override
    public String replay(String endpoint, byte[] serializedInput) {
      NexusServiceStub stub =
          Workflow.newUntypedNexusServiceStub(
              TransferService.NAME,
              NexusServiceOptions.newBuilder()
                  .setEndpoint(endpoint)
                  .setOperationOptions(
                      NexusOperationOptions.newBuilder()
                          .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                          .build())
                  .build());
      RawValue input;
      try {
        input = new RawValue(io.temporal.api.common.v1.Payload.parseFrom(serializedInput));
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(e);
      }
      try {
        NexusOperationHandle<Object> handle =
            stub.start(TransferService.TRANSFER, Object.class, input);
        return "started:" + handle.getExecution().get().getOperationToken().orElse("");
      } catch (RuntimeException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
          if (t instanceof HandlerException) {
            return "rejected:" + ((HandlerException) t).getErrorType();
          }
        }
        StringBuilder chain = new StringBuilder("failed:");
        for (Throwable t = e; t != null; t = t.getCause()) {
          chain
              .append(t.getClass().getSimpleName())
              .append('(')
              .append(t.getMessage())
              .append(")<-");
        }
        return chain.toString();
      }
    }
  }

  /** Adds attacker-chosen Nexus headers to every Nexus call the attacker makes. */
  static final class AttackerHeaderInterceptor extends WorkerInterceptorBase {
    static final Map<String, String> HEADERS = new ConcurrentHashMap<>();

    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
      return new WorkflowInboundCallsInterceptorBase(next) {
        @Override
        public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
          super.init(
              new WorkflowOutboundCallsInterceptorBase(outboundCalls) {
                @Override
                public <R> ExecuteNexusOperationOutput<R> executeNexusOperation(
                    ExecuteNexusOperationInput<R> input) {
                  input.getHeaders().putAll(HEADERS);
                  return super.executeNexusOperation(input);
                }
              });
        }
      };
    }
  }
}
