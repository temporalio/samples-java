# Demo tasks interaction

This example demonstrate a generic implementation for tasks interaction in Temporal.

The basic implementation consist on three parts:
- One activity (or local activity) that send the request (create a task).
- Block the workflow execution `Workflow.await` awaiting a Signal.
- The workflow will eventually receive a signal that unblocks it.

Additionally, the example allows to track task state (PENDING, STARTED, COMPLETED...).

> If the client can not send a Signal to the workflow execution, steps 2 and 3 can be replaced by an activity
that polls using one of [these three strategies](../polling).

## Run the sample

- Start the worker

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.worker.Worker
```

- Schedule workflow execution 

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.StartWorkflow
```

- Update task

Update one of the open task to the next state (PENDING -> STARTED -> COMPLETED)
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.UpdateTask
```

The workflow has three task, each task has three different states and is created in PENDING state. 
You will have to run this class six times to move each task from PENDING to STARTED and to COMPLETED. 
Once the last task is completed the workflow completes.
