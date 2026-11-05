# Demonstrates Signal Batching

Demonstrates a situation when a single deposit should be initiated for multiple withdrawals.
For example, a seller might want to be paid once per fixed number of transactions.
The sample can be easily extended to perform a payment based on more complex criteria like a specific time or accumulated amount.

The sample also demonstrates the *signal with start* way of starting Workflows.
If the Workflow is already running, it just receives the Signal. If it is not running, then it is started first, and then the signal is delivered to it.
You can think about *signal with start* as a lazy way to create Workflows when signaling them.

Each Workflow Execution settles exactly one batch. The Signal handler queues requests and ignores
duplicate reference IDs. Once a full batch is available, the Workflow selects exactly `batchSize`
requests, executes their withdrawals in parallel, and deposits their combined amount. Its completed
withdrawal count cannot exceed the batch size, so the `count == batchSize` condition remains valid.

Requests received while withdrawals or the deposit are running stay in the queue without executing
their withdrawals. After a successful deposit, the Workflow completes if the queue is empty.
Otherwise, it uses Continue-As-New to pass the pending requests and reference IDs to the next
execution. For example, with a batch size of five and six unique requests, the first execution
settles five and the next waits for four more requests before withdrawing and settling its batch.
Waiting for that full batch is expected; a client waiting for the Workflow result also follows
Continue-As-New and waits until the execution chain completes.

`TransferRequester` supplies an empty `BatchState` on the initial start. A continued execution uses
the carried state, initialized before any Signals are handled. Reference IDs are deduplicated
throughout that execution chain; a separate start after completion begins with fresh state.

This implementation changes Activity scheduling and does not preserve replay compatibility with
executions started by the original implementation. Use it for new executions; upgrading existing
executions requires a separate versioning or migration strategy.

Reference IDs remain in the carried state for the whole execution chain. Continue-As-New limits
history per execution but does not bound this state or the pending queue. Bounding reference
retention would require choosing a finite deduplication window or using external durable storage.

**How to run the Money Batch Sample**

Money Batch sample has three separate processes. One to host Workflow Executions,
another to host Activity Executions, and the third one to request transfers (start Workflow Executions).

Start Workflow Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.AccountTransferWorker
```

Start Activity Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.AccountActivityWorker
```

Execute at least three times to request three transfers (example batch size):

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.TransferRequester
```
