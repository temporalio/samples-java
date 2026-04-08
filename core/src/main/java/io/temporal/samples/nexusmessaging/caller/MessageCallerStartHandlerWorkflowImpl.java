package io.temporal.samples.nexusmessaging.caller;

import io.temporal.samples.nexusmessaging.service.SampleNexusService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageCallerStartHandlerWorkflowImpl implements MessageCallerStartHandlerWorkflow {

  private static final Logger logger =
      LoggerFactory.getLogger(MessageCallerStartHandlerWorkflowImpl.class);

  private final String workflowId = "Remote Start Workflow";

  SampleNexusService sampleNexusService =
      Workflow.newNexusServiceStub(
          SampleNexusService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public String sentMessage() {
    /*
    SampleNexusService.QueryWorkflowOutput queryOutput =
        sampleNexusService.queryWorkflow(
            new SampleNexusService.QueryWorkflowInput("query string going in"));
    logger.info("Query output: {}", queryOutput.getMessage());

    SampleNexusService.UpdateWorkflowOutput updateOutput =
        sampleNexusService.updateWorkflow(
            new SampleNexusService.UpdateWorkflowInput("update input"));
    logger.info("Update output: {}", updateOutput.getResult());

    sampleNexusService.signalWorkflow(new SampleNexusService.SignalWorkflowInput("signal input"));
    logger.info("Signal sent via Nexus");

    return "Query: " + queryOutput.getMessage() + ", Update: " + updateOutput.getResult();

    */

    NexusOperationHandle<SampleNexusService.RunFromRemoteOutput> handle =
        Workflow.startNexusOperation(
            sampleNexusService::runFromRemote,
            new SampleNexusService.RunFromRemoteInput(workflowId));
    // Optionally wait for the operation to be started. NexusOperationExecution will contain the
    // operation token in case this operation is asynchronous.
    handle.getExecution().get();

    SampleNexusService.QueryWorkflowOutput queryWorkflowOutput =
        sampleNexusService.queryWorkflowRemoteStart(
            new SampleNexusService.QueryWorkflowRemoteStartInput(
                "query string going in", workflowId));
    logger.info("Caller has query output of {}", queryWorkflowOutput);

    sampleNexusService.signalWorkflowRemoteStart(
        new SampleNexusService.SignalWorkflowRemoteStartInput("signal input", workflowId));
    logger.info("Signal sent via Nexus");

    return handle.getResult().get().getMessage();
  }
}
