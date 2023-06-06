Demonstrates how to route tasks to specific Workers. This sample has a set of Activities that download a file, processes it, and uploads the result to a destination. Any Worker can execute the first Activity. However, the second and third Activities must be executed on the same host as the first one.

####  Running the File Processing Sample

The sample has two executables. Execute each command in a separate terminal window.


The first command runs the Worker that hosts the Workflow and Activity Executions. To demonstrate that Activities execute together, we recommend running more than one instance of this Worker.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingWorker
```

The second command start the Workflow Execution. Each time the command runs, it starts a new Workflow Execution.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingStarter
```
