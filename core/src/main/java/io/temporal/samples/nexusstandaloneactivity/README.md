## Nexus Operation Backed by a Standalone Activity

> [!WARNING]
> Standalone Nexus Operations are in pre-release and may be subject to backwards-incompatible changes.
> They require a server version that supports this feature. Use the dev server build at:
> https://github.com/temporalio/cli/releases/tag/v1.7.4-standalone-nexus-operations.

This sample shows how to implement a Nexus operation whose backing execution is a **standalone
Activity**. `TemporalOperationHandler` maps the Temporal execution onto the Nexus operation:
starting the operation starts the Activity, and when the Activity finishes Temporal delivers its
result to the Nexus caller.

### Sample structure

| File                                                                               | Purpose |
|------------------------------------------------------------------------------------|---|
| [`service/GreetingNexusService.java`](./service/GreetingNexusService.java)         | Nexus service definition shared by caller and handler |
| [`handler/GreetingActivityImpl.java`](./handler/GreetingActivityImpl.java)         | The standalone Activity backing the operation |
| [`handler/GreetingNexusServiceImpl.java`](./handler/GreetingNexusServiceImpl.java) | Operation implementation, via `TemporalOperationHandler.create` and `startActivity` |
| [`handler/HandlerWorker.java`](./handler/HandlerWorker.java)                       | Worker hosting the Nexus handler and the Activity |
| [`ClientStarter.java`](./ClientStarter.java)                                       | Executes the Nexus operation from client code |

The starter and worker connect to two different namespaces (a "caller" namespace and a "handler"
namespace) — this mirrors how Nexus is typically used to cross namespace boundaries. The client is
configured via the SDK's [environment configuration](https://docs.temporal.io/develop/environment-configuration)
support (`ClientConfigProfile.load()`), which reads `TEMPORAL_NAMESPACE`, `TEMPORAL_ADDRESS`, etc.
from the environment (and optionally a profile from `temporal.toml`).

### Run locally against a dev server

1. Start the [Temporal dev server build that supports standalone Nexus operations](https://docs.temporal.io/standalone-nexus-operation#temporal-cli-support)
   with the required namespaces pre-created and Activity callbacks enabled:

   ```bash
   ./temporal server start-dev \
     --dynamic-config-value activity.enableCallbacks=true \
     --namespace my-caller-namespace \
     --namespace my-handler-namespace
   ```

2. Create a Nexus endpoint that routes to the handler namespace and the worker's task queue:

   ```bash
   ./temporal operator nexus endpoint create \
     --name my-nexus-endpoint \
     --target-namespace my-handler-namespace \
     --target-task-queue nexus-handler-queue
   ```

3. In a second terminal, start the handler worker in the handler namespace:

   ```bash
   TEMPORAL_NAMESPACE=my-handler-namespace \
     ./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandaloneactivity.handler.HandlerWorker
   ```

4. In a third terminal, run the starter in the caller namespace:

   ```bash
   TEMPORAL_NAMESPACE=my-caller-namespace \
     ./gradlew -q :core:execute -PmainClass=io.temporal.samples.nexusstandaloneactivity.ClientStarter
   ```

Expected output:

```text
Hello, World!
```
