### Early-Return Sample

This sample demonstrates an early-return from a workflow.

By utilizing Update-with-Start, a client can start a new workflow and synchronously receive 
a response mid-workflow, while the workflow continues to run to completion.

To run the sample, start the worker:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.earlyreturn.EarlyReturnWorker
```

Then, start the client:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.earlyreturn.EarlyReturnClient
```

* The client will start a workflow using Update-With-Start.
* Update-With-Start will trigger an initialization step.
* If the initialization step succeeds (default), intialization will return to the client with a transaction ID and the workflow will continue. The workflow will then complete and return the final result.
* If the intitialization step fails (amount <= 0), the workflow will return to the client with an error message and the workflow will run an activity to cancel the transaction.

To trigger a failed initialization, set the amount to <= 0 in the `EarlyReturnClient` class's `runWorkflowWithUpdateWithStart` method and re-run the client.