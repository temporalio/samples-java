package io.temporal.samples.nexusstandaloneactivity.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.StartActivityOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.TemporalOperationHandler;
import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService;
import java.time.Duration;

// Implements the GreetingNexusService operation on top of a standalone Activity.
@ServiceImpl(service = GreetingNexusService.class)
public class GreetingNexusServiceImpl {

  // TemporalOperationHandler.create maps a Temporal execution onto a Nexus operation. Here the
  // execution is a standalone Activity: startActivity returns an asynchronous operation result, so
  // the Nexus operation stays running until the Activity completes, at which point Temporal
  // delivers the Activity's result to the Nexus caller.
  @OperationImpl
  public OperationHandler<GreetingNexusService.GreetingInput, GreetingNexusService.GreetingOutput>
      greet() {
    return TemporalOperationHandler.create(
        (ctx, client, input) ->
            client.startActivity(
                GreetingActivity.class,
                GreetingActivity::createGreeting,
                input,
                StartActivityOptions.newBuilder()
                    // Use a business identifier from the operation input so callers can identify
                    // the same Activity independently of any individual Nexus request.
                    .setId(getActivityId(input))
                    // The task queue is required. This sample runs the Activity on the same queue
                    // as the Nexus Worker that is handling this operation.
                    .setTaskQueue(Nexus.getOperationContext().getInfo().getTaskQueue())
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .build()));
  }

  static String getActivityId(GreetingNexusService.GreetingInput input) {
    return "greeting-" + input.getName();
  }
}
