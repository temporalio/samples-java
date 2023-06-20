# Workflow execution with mTLS

This example shows how to secure your Temporal application with [mTLS](https://docs.temporal.io/security/#encryption-in-transit-with-mtls).
This is required to connect with Temporal Cloud or any production Temporal deployment.


## Export env variables

Before running the example you need to export the following env variables: 

- TEMPORAL_ENDPOINT: grpc endpoint, for Temporal Cloud would like `${namespace}.tmprl.cloud:7233`.
- TEMPORAL_NAMESPACE: Namespace.
- TEMPORAL_CLIENT_CERT: For Temporal Cloud see requirements [here](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud#end-entity-certificates).
- TEMPORAL_CLIENT_KEY: For Temporal Cloud see requirements [here](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud#end-entity-certificates).

## Running this sample

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.ssl.Starter
```

## Run SslEnabledWorkerCustomCA Sample

This sample shows how to start a worker that connects to a temporal cluster with mTLS enabled; created by ([tls-simple sample](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple));

SslEnabledWorkerCustomCA demonstrates:

- Passing a custom CA certificate file as parameter
- Overriding the authority name used for TLS handshakes (if needed)

1.Start a temporal cluster with tls

Please follow the temporal server-sample to start simple Temporal mTLS cluster locally: [tls-simple](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)

2.Set environment variables

```bash
# Environment variables
# paths to ca cert, client cert and client key come from the previous step 
export TEMPORAL_CLIENT_CERT="</path/to/client.pem>"
export TEMPORAL_CLIENT_KEY="</path/to/client.key>"
export TEMPORAL_CA_CERT="</path/to/ca.cert>"    
export TEMPORAL_ENDPOINT="localhost:7233"    # Temporal grpc endpoint       
export TEMPORAL_NAMESPACE="default"          # Temporal namespace            
export TEMPORAL_SERVER_HOSTNAME="tls-sample" # Temporal server host name  
```

3.Start the Worker

```bash
./gradlew -q execute -PmainClass="io.temporal.samples.ssl.SslEnabledWorkerCustomCA"
```

4.Expected result

```text
[main] INFO  i.t.s.WorkflowServiceStubsImpl - Created WorkflowServiceStubs for channel: ManagedChannelOrphanWrapper{delegate=ManagedChannelImpl{logId=1, target=localhost:7233}} 
[main] INFO  io.temporal.internal.worker.Poller - start: Poller{name=Workflow Poller taskQueue="MyTaskQueue", namespace="default"} 
Workflow completed:done 
```
