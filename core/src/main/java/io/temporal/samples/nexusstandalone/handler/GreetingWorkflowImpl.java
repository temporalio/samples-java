package io.temporal.samples.nexusstandalone.handler;

import io.temporal.samples.nexusstandalone.service.GreetingNexusService;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

public class GreetingWorkflowImpl implements GreetingWorkflow {
  private static final Logger logger = Workflow.getLogger(GreetingWorkflowImpl.class);

  @Override
  public GreetingNexusService.GreetingOutput greet(GreetingNexusService.GreetingInput input) {
    logger.info(
        "Greeting workflow started for {}; blocking until cancelled or terminated",
        input.getName());
    // This workflow exists only to keep the backing standalone Nexus operation in a running state
    // long enough for the sample to demonstrate describe/cancel/terminate. It blocks forever and
    // never completes on its own.
    Workflow.await(() -> false);

    throw Workflow.wrap(
        new IllegalStateException("greeting workflow should never complete normally"));
  }
}
