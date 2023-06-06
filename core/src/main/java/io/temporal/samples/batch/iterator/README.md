A sample implementation of the Workflow iterator pattern.

A workflow starts a configured number of Child Workflows in parallel. Each child processes a single record. 
After all children complete, the parent calls continue-as-new and starts the children for the next page of records. 

This allows processing a set of records of any size. The advantage of this approach is simplicity. 
The main disadvantage is that it processes records in batches, with each batch waiting for the slowest child workflow.

A variation of this pattern runs activities instead of child workflows.

#### Running the Iterator Batch Sample

The sample has two executables. Execute each command in a separate terminal window.

The first command runs the Worker that hosts the Workflow and Activity Executions.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.batch.iterator.IteratorBatchWorker
```

The second command start the Workflow Execution. Each time the command runs, it starts a new Workflow Execution.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.batch.iterator.IteratorBatchStarter
```
