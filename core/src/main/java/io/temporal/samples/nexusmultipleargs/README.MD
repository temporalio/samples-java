# Nexus Multiple Arguments Sample

This sample shows how to map a Nexus operation to a caller workflow that takes multiple input arguments using [WorkflowRunOperation.fromWorkflowHandle](https://javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/nexus/WorkflowRunOperation.html#fromWorkflowHandle(io.temporal.nexus.WorkflowHandleFactory)).

To run this sample, set up your environment following the instructions in the main [Nexus Sample](../nexus/README.md).

In separate terminal windows:

### Nexus handler worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusmultipleargs.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

### Nexus caller worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusmultipleargs.caller.CallerWorker \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Start caller workflow

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusmultipleargs.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Output

which should result in:
```
[main] INFO  i.t.s.nexus.caller.CallerStarter - Workflow result: Nexus Echo 👋 
[main] INFO  i.t.s.nexus.caller.CallerStarter - Workflow result: ¡Hola! Nexus 👋  
```
