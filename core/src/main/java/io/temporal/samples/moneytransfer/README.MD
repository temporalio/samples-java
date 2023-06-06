The Money Transfer sample has three separate processes.
One to host Workflow Executions, another to host Activity Executions, and the third one to request transfers (start Workflow Executions).

Start Workflow Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.AccountTransferWorker
```

Start Activity Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.AccountActivityWorker
```

Execute once per requested transfer:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.TransferRequester
```
