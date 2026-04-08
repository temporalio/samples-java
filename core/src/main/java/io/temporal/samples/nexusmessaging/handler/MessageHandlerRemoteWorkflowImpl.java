package io.temporal.samples.nexusmessaging.handler;

import io.temporal.samples.nexusmessaging.service.SampleNexusService;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandlerRemoteWorkflowImpl implements MessageHandlerRemoteWorkflow {
  private static final Logger logger =
      LoggerFactory.getLogger(MessageHandlerRemoteWorkflowImpl.class);
  private boolean keepRunning = true;

  @Override
  public SampleNexusService.RunFromRemoteOutput runFromRemote(
      SampleNexusService.RunFromRemoteInput input) {
    Workflow.await(() -> !keepRunning);

    String logMessage = "runFromRemote was told to stop, and did.";
    logger.info(logMessage);
    return new SampleNexusService.RunFromRemoteOutput(logMessage);
  }

  @Override
  public SampleNexusService.QueryWorkflowOutput queryWorkflow(
      SampleNexusService.QueryWorkflowInput name) {
    logger.info("Query '{}' was received", name.getName());
    return new SampleNexusService.QueryWorkflowOutput("Query received");
  }

  @Override
  public void signalWorkflow(SampleNexusService.SignalWorkflowInput name) {
    logger.info("Signal was received");
    keepRunning = false;
  }

  @Override
  public SampleNexusService.UpdateWorkflowOutput updateWorkflow(
      SampleNexusService.UpdateWorkflowInput name) {
    logger.info("Update {} was received", name.getName());
    return new SampleNexusService.UpdateWorkflowOutput(10);
  }

  @Override
  public void setLanguageValidator(SampleNexusService.UpdateWorkflowInput name) {
    if (name.getName().equals("invalid")) {
      logger.info("Update {} was rejected", name.getName());
      throw new IllegalArgumentException("Invalid update name!");
    }
    logger.info("Update {} was validated", name.getName());
  }
}
