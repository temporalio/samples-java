package io.temporal.samples.nexus_messaging.callerpattern.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.nexus.Nexus;
import io.temporal.samples.nexus_messaging.callerpattern.service.Language;
import io.temporal.samples.nexus_messaging.callerpattern.service.NexusGreetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus operation handler implementation. Each operation receives a userId, which is mapped to a
 * workflow ID using {@link #WORKFLOW_ID_PREFIX}. The operations are synchronous because queries and
 * updates against a running workflow complete quickly.
 */
@ServiceImpl(service = NexusGreetingService.class)
public class NexusGreetingServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(NexusGreetingServiceImpl.class);

  static final String WORKFLOW_ID_PREFIX = "GreetingWorkflow_for_";

  // This example assumes you might have multiple workflows, one for each user.
  // If you had a single workflow for all users, then you could remove the
  // getWorkflowId method, remove the user ID from each input, and just
  // use the single worflow ID in the getWorkflowStub method below.
  public static String getWorkflowId(String userId) {
    return WORKFLOW_ID_PREFIX + userId;
  }

  private GreetingWorkflow getWorkflowStub(String userId) {
    return Nexus.getOperationContext()
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, getWorkflowId(userId));
  }

  @OperationImpl
  public OperationHandler<
          NexusGreetingService.GetLanguagesInput, NexusGreetingService.GetLanguagesOutput>
      getLanguages() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguages was received for user {}", input.getUserId());
          return getWorkflowStub(input.getUserId()).getLanguages(input);
        });
  }

  @OperationImpl
  public OperationHandler<NexusGreetingService.GetLanguageInput, Language> getLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Query for GetLanguage was received for user {}", input.getUserId());
          return getWorkflowStub(input.getUserId()).getLanguage();
        });
  }

  // Routes to setLanguageUsingActivity (not setLanguage) so that new languages not already in the
  // greetings map can be fetched via an activity.
  @OperationImpl
  public OperationHandler<NexusGreetingService.SetLanguageInput, Language> setLanguage() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Update for SetLanguage was received for user {}", input.getUserId());
          return getWorkflowStub(input.getUserId()).setLanguageUsingActivity(input);
        });
  }

  @OperationImpl
  public OperationHandler<NexusGreetingService.ApproveInput, NexusGreetingService.ApproveOutput>
      approve() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          logger.info("Signal for Approve was received for user {}", input.getUserId());
          getWorkflowStub(input.getUserId()).approve(input);
          return new NexusGreetingService.ApproveOutput();
        });
  }
}
