# Demonstrates Signal Batching

Demonstrates a situation when a single deposit should be initiated for multiple withdrawals.
For example, a seller might want to be paid once per fixed number of transactions.
The sample can be easily extended to perform a payment based on more complex criteria like a specific time or accumulated amount.

The sample also demonstrates the *signal with start* way of starting Workflows.
If the Workflow is already running, it just receives the Signal. If it is not running, then it is started first, and then the signal is delivered to it.
You can think about *signal with start* as a lazy way to create Workflows when signaling them.

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
