# Nexus Cancellation

This sample shows how to cancel a Nexus operation from a caller workflow and specify a cancellation type. In this sample we will show using the `WAIT_REQUESTED` cancellation type, which allows the caller to return after the handler workflow has received the requested to be cancelled, but does not wait for the handler workflow to finish processing the cancellation request.

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

which should result in on the caller side:
```
14:33:52.810 i.t.s.n.caller.CallerStarter - Started workflow workflowId: 87e97bf0-ca8a-4ae6-a9dc-ae97e5c0ac41 runId: 01976b36-a524-71a1-b848-8eb385fec2c3 
14:33:54.250 i.t.s.n.caller.CallerStarter - Workflow result: Hallo Nexus 👋 
```

on the handler side:

```
14:33:54.177 INFO  i.t.s.n.h.HelloHandlerWorkflowImpl - HelloHandlerWorkflow was cancelled successfully. 
14:33:56.167 INFO  i.t.s.n.h.HelloHandlerWorkflowImpl - HelloHandlerWorkflow was cancelled successfully. 
14:33:57.172 INFO  i.t.s.n.h.HelloHandlerWorkflowImpl - HelloHandlerWorkflow was cancelled successfully. 
14:33:57.176 INFO  i.t.s.n.h.HelloHandlerWorkflowImpl - HelloHandlerWorkflow was cancelled successfully. 
```

Notice the timing, the caller workflow returned before the handler workflow was cancelled. This is because of the use of `WAIT_REQUESTED` as the cancellation type in the Nexus operation. This means the caller didn't have to wait for the handler workflow to finish, but still guarantees the handler workflow will receive the cancellation request. 