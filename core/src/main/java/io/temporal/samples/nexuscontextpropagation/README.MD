# Nexus Context Propagation

This sample shows how to propagate MDC context values from Workflows to Nexus operations. 
Nexus does not support `ContextPropagator` since the header format is not compatible. Users should look at `NexusMDCContextInterceptor` for propagating MDC context values.

From more details on Nexus and how to setup to run this samples please see the [Nexus Sample](../nexus/README.MD).

In separate terminal windows:

### Nexus handler worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexuscontextpropagation.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

### Nexus caller worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexuscontextpropagation.caller.CallerWorker \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Start caller workflow

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexuscontextpropagation.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Output

which should result in this on the caller side:
```
INFO  i.t.s.n.caller.CallerStarter - Started EchoCallerWorkflow workflowId: 7ac97cb9-b457-4052-af94-d82478c35c5e runId: 01954eb9-6963-7b52-9a1d-b74e64643846 
INFO  i.t.s.n.caller.CallerStarter - Workflow result: Nexus Echo 👋 
INFO  i.t.s.n.caller.CallerStarter - Started HelloCallerWorkflow workflowId: 9e0bc89c-5709-4742-b7c0-868464c2fccf runId: 01954eb9-6ae3-7d6d-b355-71545688309d 
INFO  i.t.s.n.caller.CallerStarter - Workflow result: Hello Nexus 👋 
```

And this on the handler side:
```
INFO  i.t.s.n.handler.NexusServiceImpl - Echo called from a workflow with ID : 7ac97cb9-b457-4052-af94-d82478c35c5e 
INFO  i.t.s.n.h.HelloHandlerWorkflowImpl - HelloHandlerWorkflow called from a workflow with ID : 9e0bc89c-5709-4742-b7c0-868464c2fccf 
```
