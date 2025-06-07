# Nexus Early Return

This sample demonstrates the early return pattern with Nexus. 

For more details on the early return pattern see the [Early Return Sample](../earlyreturn/README.md)

To run this sample, set up your environment following the instructions in the main [Nexus Sample](../nexus/README.md).

In separate terminal windows:

### Nexus handler worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusEarlyReturn.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

### Nexus caller worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusEarlyReturn.caller.CallerWorker \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Start caller workflow

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusEarlyReturn.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Output

which should result in:
```
10:52:08.542 { } [main] INFO  i.t.s.n.caller.CallerStarter - Started TransferWorkflow workflowId: 1526a62a-cc76-49a9-bc10-b91d67b4f0e8 runId: 01974b85-7b59-76e5-80aa-3ab0dfb983ef 
10:52:12.084 { } [main] INFO  i.t.s.n.caller.CallerStarter - Workflow result: Transaction completed successfully. 
```
