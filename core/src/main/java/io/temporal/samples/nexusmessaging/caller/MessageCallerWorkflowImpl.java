package io.temporal.samples.nexusmessaging.caller;

import io.temporal.samples.nexusmessaging.service.SampleNexusService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageCallerWorkflowImpl implements MessageCallerWorkflow {

  private static final Logger logger = LoggerFactory.getLogger(MessageCallerWorkflowImpl.class);

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
  }
}
