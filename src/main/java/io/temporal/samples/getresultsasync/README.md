# Get Workflow results async

This sample shows the use of WorkflowStub.getResult and WorkflowStub.getResultAsync
to show how the Temporal client api can not only start workflows async but also wait for their results 
async as well.

## Run the sample

1. Start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.getresultsasync.Worker
```

2. Start the Starter

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.getresultsasync.Starter
```