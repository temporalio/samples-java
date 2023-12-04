# The Retry On Signal Interceptor

Demonstrates an interceptor that upon activity failure waits for an external signal that indicates if activity should
fail or retry.

Starts Worker. The worker upon start initiates a workflow that has an activity that fails on the fist invocation.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.retryonsignalinterceptor.MyWorkflowWorker
```

Sends Signal to indicate that the activity should be retried.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.retryonsignalinterceptor.RetryRequester
```

Sends a signal to propagate the activity failure to the workflow instead of retrying.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.retryonsignalinterceptor.FailureRequester
```
