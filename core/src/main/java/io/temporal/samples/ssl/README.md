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
