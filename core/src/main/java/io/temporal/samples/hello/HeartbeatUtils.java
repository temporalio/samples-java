package io.temporal.samples.hello;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.client.ActivityCompletionException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import java.text.MessageFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public class HeartbeatUtils {
  //  private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatUtils.class);

  // withBackgroundHeartbeatAndActivity runs the underlying activity Callable in a thread and
  // heartbeats in another thread
  // Cancellation can be ignored by returning `true` from the `shouldIgnoreCancel` predicate,
  // otherwise the
  // activity Callable is cancelled and a Cancellation failure is thrown.
  // Callers should handle `ApplicationFailure` if you are allowing cancellation and determine
  // if you want to exit the Activity with or without the failure bubbling up to the Workflow.
  public static <T> T withBackgroundHeartbeatAndActivity(
      final Supplier<ActivityExecutionContext> activityContext,
      final Callable<T> callable,
      final int heartbeatIntervalSeconds,
      final Predicate<Callable<T>> shouldIgnoreCancel)
      throws CanceledFailure {

    var context = activityContext.get();
    var logger =
        LoggerFactory.getLogger(
            MessageFormat.format(
                "{0}/{1}", HeartbeatUtils.class.getName(), context.getInfo().getActivityId()));
    final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    final ScheduledExecutorService activityExecutor = Executors.newSingleThreadScheduledExecutor();
    var activityInvocation = activityExecutor.schedule(callable, 0, TimeUnit.SECONDS);
    final AtomicReference<Runnable> canceller =
        new AtomicReference<>(
            () -> {
              logger.warn("canceller is running...");
              activityInvocation.cancel(true);
            });

    var unused =
        heartbeatExecutor.scheduleAtFixedRate(
            () -> {
              try {
                logger.info("heartbeating...");
                context.heartbeat(null);
              } catch (ActivityCompletionException e) {
                logger.warn("received cancellation", e);
                try {
                  if (shouldIgnoreCancel == null || !shouldIgnoreCancel.test(callable)) {
                    // cancellation should be accepted so cancel the invocation and rethrow the e
                    canceller.get().run();
                    throw e;
                  } else {
                    logger.warn("Activity Cancellation ignored so keep heartbeating...");
                  }
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }
            },
            0,
            heartbeatIntervalSeconds,
            TimeUnit.SECONDS);

    try {
      return activityInvocation.get();
    } catch (CancellationException e) {
      logger.warn("Canceled activity invocation", e);
      // Opinionated way to keep Workflow from retrying this activity that is no longer going to
      // heartbeat.
      // if we don't returning a "non-retryable" failure, you will see Heartbeat timeout failures
      // but really
      // we want to communicate that the activity has been canceled and allow the caller to handle
      // the exception.
      // We could just rethrow the CancellationException here but then every user of this utility
      // would have to convert to a nonretryable error.
      throw ApplicationFailure.newNonRetryableFailureWithCause(
          e.getMessage(), e.getClass().getTypeName(), e);
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      // regardless of whether the activity ignores cancellation using `onCancel` or continued,
      // shutdown at last
      activityExecutor.shutdown();
      heartbeatExecutor.shutdown();
    }
  }
}
