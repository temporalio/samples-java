package io.temporal.samples.hello;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.client.ActivityCompletionException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatUtils.class);

  public static <T> T withBackgroundHeartbeatAndActivity(
      final AtomicReference<Runnable> cancellationCallbackRef,
      final Callable<T> callable,
      final Supplier<ActivityExecutionContext> activityContext,
      final int heartbeatIntervalSeconds)
      throws ExecutionException {

    var context = activityContext.get();
    final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    final ScheduledExecutorService activityExecutor = Executors.newSingleThreadScheduledExecutor();
    var activityInvocation = activityExecutor.schedule(callable, 0, TimeUnit.SECONDS);
    final AtomicReference<Runnable> canceller =
        new AtomicReference<>(
            () -> {
              LOGGER.warn("canceller is running...");
              activityInvocation.cancel(true);
              if (cancellationCallbackRef != null) {
                cancellationCallbackRef.get().run();
              }
            });

    var unused =
        heartbeatExecutor.scheduleAtFixedRate(
            () -> {
              try {
                LOGGER.info("heartbeating...");
                context.heartbeat(null);
              } catch (ActivityCompletionException e) {
                LOGGER.warn("received cancellation", e);
                canceller.get().run();
                throw e;
              }
            },
            0,
            heartbeatIntervalSeconds,
            TimeUnit.SECONDS);

    try {
      return activityInvocation.get();
    } catch (ExecutionException e) {
      LOGGER.warn("Background heartbeated invocation interrupt {}", e.getMessage(), e);
      throw e;
    } catch (InterruptedException e) {
      throw new ExecutionException(e);
    } catch (CancellationException e) {
      LOGGER.warn("Cancellation exception", e);
      throw new ExecutionException(e);
    } finally {
      activityExecutor.shutdown();
      heartbeatExecutor.shutdown();
    }
  }
}
