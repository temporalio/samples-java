# Workflow execution with API Key

This example shows how to secure your Temporal application with API Key authentication.
This is required to connect with Temporal Cloud or any production Temporal deployment that uses API Key authentication.

## Prerequisites

1. A Temporal Cloud account
2. A namespace in Temporal Cloud
3. An API Key for your namespace

## Getting your API Key

1. Log in to your Temporal Cloud account
2. Navigate to your namespace
3. Go to Namespace Settings > API Keys
4. Click "Create API Key"
5. Give your API Key a name and select the appropriate permissions
6. Copy the API Key value (you won't be able to see it again)

## Export env variables

Before running the example you need to export the following env variables: 

```bash
# Your Temporal Cloud endpoint (e.g., us-east-1.aws.api.temporal.io:7233)
export TEMPORAL_ENDPOINT="us-east-1.aws.api.temporal.io:7233"

# Your Temporal Cloud namespace
export TEMPORAL_NAMESPACE="your-namespace"

# Your API Key from Temporal Cloud
export TEMPORAL_API_KEY="your-api-key"
```

## Running this sample

This sample consists of two components that need to be run in separate terminals:

1. First, start the worker:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.apikey.ApiKeyWorker
```

2. Then, in a new terminal, run the starter:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.apikey.Starter
```

## Expected result

When running the worker, you should see:
```text
[main] INFO  i.t.s.WorkflowServiceStubsImpl - Created WorkflowServiceStubs for channel: ManagedChannelOrphanWrapper{delegate=ManagedChannelImpl{logId=1, target=us-east-1.aws.api.temporal.io:7233}} 
[main] INFO  io.temporal.internal.worker.Poller - start: Poller{name=Workflow Poller taskQueue="MyTaskQueue", namespace="your-namespace"} 
Worker started. Press Ctrl+C to exit.
```

When running the starter, you should see:
```text
[main] INFO  i.t.s.WorkflowServiceStubsImpl - Created WorkflowServiceStubs for channel: ManagedChannelOrphanWrapper{delegate=ManagedChannelImpl{logId=1, target=us-east-1.aws.api.temporal.io:7233}} 
[main] INFO  io.temporal.internal.worker.Poller - start: Poller{name=Workflow Poller taskQueue="MyTaskQueue", namespace="your-namespace"} 
done
```

## Troubleshooting

If you encounter any issues:

1. Verify your environment variables are set correctly:
   ```bash
   echo $TEMPORAL_ENDPOINT
   echo $TEMPORAL_NAMESPACE
   echo $TEMPORAL_API_KEY
   ```

2. Check that your API Key has the correct permissions for your namespace

3. Ensure your namespace is active and accessible

4. If you get connection errors, verify your endpoint is correct and accessible from your network

5. Make sure you're running the commands from the correct directory (where the `gradlew` script is located)
