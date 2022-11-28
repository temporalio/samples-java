A sample implementation of processing a batch by an activity.

An activity can run as long as needed. It reports that it is still alive through heartbeat. If the worker is restarted
the activity is retried after the heartbeat timeout. Temporal allows store data in heartbeat _details_. These details
are available to the next activity attempt. The progress of the record processing is stored in the details to avoid
reprocessing records from the beginning on failures.

#### Running the Iterator Batch Sample

The sample has two executables. Execute each command in a separate terminal window.

The first command runs the Worker that hosts the Workflow and Activity Executions. Restart the worker while the batch is
executing to see how activity timeout and retry work.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.batch.heartbeatingactivity.HeartbeatingActivityBatchWorker
```

The second command start the Workflow Execution. Each time the command runs, it starts a new Workflow Execution.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.batch.heartbeatingactivity.HeartbeatingActivityBatchStarter
```
