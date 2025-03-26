# Nexus Cancellation

This sample shows how to cancel a Nexus operation from a caller workflow.

To run this sample, set up your environment following the instructions in the main [Nexus Sample](../nexus/README.md).

Next, in separate terminal windows:

### Nexus handler worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexuscancellation.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

### Nexus caller worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexuscancellation.caller.CallerWorker \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Start caller workflow

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexuscancellation.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Output

which should result in:
```
INFO  i.t.s.n.caller.CallerStarter - Started workflow workflowId: 326732dd-a2b1-4de7-9ddd-dcee4f9f0229 runId: d580499f-79d5-461d-bd49-6248b4e522ae 
INFO  i.t.s.n.caller.CallerStarter - Workflow result: Hallo Nexus 👋 
```
