

package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;

/** Interface used to dynamically register signal and query handlers from the interceptor. */
public interface RetryOnSignalInterceptorListener {

  /** Requests retry of the activities waiting after failure. */
  @SignalMethod
  void retry();

  /** Requests no more retries of the activities waiting after failure. */
  @SignalMethod
  void fail();

  /** Returns human status of the pending activities. */
  @QueryMethod
  String getPendingActivitiesStatus();
}
