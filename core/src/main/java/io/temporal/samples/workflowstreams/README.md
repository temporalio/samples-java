# Workflow Streams

A **workflow stream** is a durable publish/subscribe log hosted inside a Temporal
workflow, provided by the experimental `io.temporal:temporal-workflowstreams`
contrib module. External code (activities, starters, other workflows) publishes
messages to named topics via **signals**; subscribers long-poll for new items via
**updates**; a **query** exposes the current offset. Because it is backed by
Temporal's durable execution, delivery is ordered, durable, and exactly-once, with
client-side batching, publisher dedup, continue-as-new survival, and truncation.

This sample mirrors the
[Go](https://github.com/temporalio/samples-go/tree/main/workflowstreams) and
[Python](https://github.com/temporalio/samples-python/tree/main/workflow_streams)
workflow streams samples. It contains six scenarios.

> **Note:** The `temporal-workflowstreams` module is merged into SDK `main` but
> not yet in a tagged release, so the repo's `javaSDKVersion` is pinned to
> `1.37.0-SNAPSHOT`, resolved automatically from the Maven Central snapshots
> repository. Drop the `-SNAPSHOT` pin once a tagged SDK release ships.

### Key APIs

Workflow side — construct a stream once in a `@WorkflowInit` constructor and publish to topics:

```java
@WorkflowInit
public MyWorkflowImpl(MyInput input) {
  stream = WorkflowStream.newInstance(input.streamState);
}

stream.topic("status").publish(new StatusEvent("received"));
```

Client side (activities, starters, external code) — publish and subscribe:

```java
try (WorkflowStreamClient client = WorkflowStreamClient.newInstance(workflowClient, workflowId);
    WorkflowStreamSubscription subscription = client.topic("status").subscribe(0)) {
  for (WorkflowStreamItem item : subscription) {
    StatusEvent evt =
        DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
            item.getPayload(), StatusEvent.class, StatusEvent.class);
  }
}
```

Non-blocking client side — subscribe with a listener instead of iterating; the
next item is delivered only after the `CompletionStage` returned from `onNext`
completes:

```java
WorkflowStreamSubscriptionHandle handle =
    client
        .topic("status")
        .subscribe(
            0,
            item -> CompletableFuture.runAsync(() -> handle(item), executor));
handle.getDoneFuture().join(); // completes when the workflow reaches a terminal state
```

Offsets are **global** across topics. To resume a subscription from where a
previous one left off, pass `subscribe` an offset one past the last item you
consumed:

```java
try (WorkflowStreamClient client = WorkflowStreamClient.newInstance(workflowClient, workflowId);
    WorkflowStreamSubscription subscription =
        client.topic("status").subscribe(lastItem.getOffset() + 1)) { // zero starts from the beginning
  for (WorkflowStreamItem item : subscription) {
    // ...
  }
}
```

### Steps to run this sample

1) Run a [Temporal service](https://github.com/temporalio/samples-java/tree/main/#How-to-use)
   (for example, `temporal server start-dev`).

2) Start the worker (serves scenarios 1–5):

```
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.StreamsWorker
```

3) Run any of the scenarios below in a separate terminal.

#### Scenario 1 — basic publish/subscribe

An order workflow publishes status events itself while an activity publishes
fine-grained progress events to the same stream. A subscriber consumes both topics.

```
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.Publisher
```

Expected output (interleaving may vary):

```
[status]   received: order=order-42
[progress] charging card...
[progress] card charged
[status]   shipped: order=order-42
[progress] charge id: charge-order-42
[status]   complete: order=order-42
workflow result: charge-order-42
```

#### Scenario 2 — non-blocking listener

Scenario 1's subscription is an iterator that parks the calling thread between
items. This scenario subscribes with a `WorkflowStreamListener` instead: `subscribe`
returns a `WorkflowStreamSubscriptionHandle` immediately, items are delivered as
callbacks, and `onNext` returns a `CompletionStage` — the next item is delivered
only once that stage completes, giving per-subscription backpressure without a
parked thread per subscription. Two order workflows are consumed concurrently,
with all items rendered on one shared executor thread, while the main thread just
waits on the handles' done futures.

```
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.NonBlockingSubscriber
```

Expected output (interleaving may vary):

```
[order-A] [status]   received
[order-B] [status]   received
[order-A] [progress] charging card...
[order-B] [progress] card charged
[order-A] [status]   shipped
...
[order-A] stream completed
[order-B] stream completed
[order-A] workflow result: charge-order-A
[order-B] workflow result: charge-order-B
```

#### Scenario 3 — reconnecting subscriber

A subscriber reads a few pipeline stage events, disconnects, then a brand-new
client resumes from the saved offset without missing events or seeing duplicates.

```
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.ReconnectingSubscriber
```

#### Scenario 4 — external publisher

The hub workflow does no work of its own; it just hosts the stream. A separate
publisher pushes news into it (using the same client factory used to subscribe) and
then signals the workflow to close. Here a publisher and subscriber run concurrently.

```
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.ExternalPublisher
```

#### Scenario 5 — truncating ticker

The ticker workflow periodically truncates old entries to bound its history, trading
complete history for a bounded log. A *fast* subscriber that reads from the start keeps
up and sees every tick. A *late* subscriber joins after truncation and resumes from a
stale offset; the stream fast-forwards it to the current base offset, so it cannot see
the truncated ticks.

```
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.TruncatingTicker
```

Expected output (the late subscriber's first line shows the fast-forward):

```
[late] requested offset 1 but it was truncated; fast-forwarded to offset 5 (skipped 4 tick(s))
[late] offset=  5  n=5
...
```

#### Scenario 6 — LLM token streaming

The workflow hosts the stream while an activity makes a streaming OpenAI call and
republishes each token delta. On a retry it emits a retry event and the subscriber
rewinds the terminal and re-renders. This scenario runs on its own worker and task
queue, and requires `OPENAI_API_KEY`.

```
# Terminal A
OPENAI_API_KEY=sk-... ./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.LlmWorker

# Terminal B
./gradlew -q execute -PmainClass=io.temporal.samples.workflowstreams.Llm -Pargs="'Explain durable execution in one sentence.'"
```
