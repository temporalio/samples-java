A sample implementation of processing a batch by an Activity.

An Activity can run as long as needed. 
It reports that it is still alive through Heartbeat. 

If the Worker is restarted, the Activity is retried after the Heartbeat Timeout. 

Temporal allows store data in Heartbeat _details_. 
These details are available to the next Activity attempt. 
The progress of the record processing is stored in the details to avoid reprocessing records from the beginning on failures.

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
