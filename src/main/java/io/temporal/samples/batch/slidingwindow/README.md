A sample implementation of a batch processing Workflow that maintains a sliding window of record processing Workflows.

A Workflow starts a configured number of Child Workflows in parallel. Each child processes a single record. 
When a child completes a new child immediately started. 

A Parent Workflow calls continue-as-new after starting a preconfigured number of children. 
A child completion is reported through a Signal as a parent cannot directly wait for a child started by a previous run.

Multiple instances of SlidingWindowBatchWorkflow run in parallel each processing a  subset of records to support higher total rate of processing.

#### Running the Sliding Window Batch Sample

The sample has two executables. Execute each command in a separate terminal window.

The first command runs the Worker that hosts the Workflow and Activity Executions.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.batch.slidingwindow.SlidingWindowBatchWorker
```

Note that `Caused by: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: UnhandledCommand` info messages in the output
are expected and benign. They ensure that signals are not lost when there is a race condition between workflow calling
continue-as-new and receiving a signal. If these messages appear too frequently consider increasing the number of
partitions parameter passed to `BatchWorkflow.processBatch`. They will completely disappear
when [Issue 1289](https://github.com/temporalio/temporal/issues/1289) is implemented.

The second command start the BatchWorkflow Execution. Each time the command runs, it starts a new BatchWorkflow
Execution.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.batch.slidingwindow.SlidingWindowBatchStarter
```
