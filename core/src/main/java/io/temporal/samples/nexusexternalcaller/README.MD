# Nexus External Caller Sample

This sample shows how to start and interact with Nexus operations from outside of a workflow.

To run this sample, set up your environment following the instructions in the main [Nexus Sample](../nexus/README.md).

Next, in separate terminal windows:

### Nexus handler worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusexternalcaller.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

### Start Nexus Operation

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexusexternalcaller.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Output

which should result in on the caller side:
```
21:18:31.282 { } [main] INFO  i.t.s.n.caller.CallerStarter - Execute echo operation: Hello 
21:18:31.299 { } [main] INFO  i.t.s.n.caller.CallerStarter - Started hello operation with token: eyJ0IjoxLCJucyI6Im15LXRhcmdldC1uYW1lc3BhY2UiLCJ3aWQiOiI3NWU0MTk2Zi05ODdiLTRhYzMtOTdiZS1hMjBiYjViOWZiNDUifQ 
21:18:31.299 { } [main] INFO  i.t.s.n.caller.CallerStarter - Waiting for hello operation to complete... 
21:18:31.311 { } [main] INFO  i.t.s.n.caller.CallerStarter - Hello operation result: Hello Hello 👋 
21:18:31.323 { } [main] INFO  i.t.s.n.caller.CallerStarter - Operation state: SUCCEEDED 
```
