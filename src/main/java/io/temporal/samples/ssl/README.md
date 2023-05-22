# Ssl enabled WorkflowClient

These samples demonstrate how to start a workflow client (worker) with SSL enabled.

## SslEnabledWorker

This sample demonstrates how to start SSL enabled worker using the `client certificate` that is issued by a trusted CA (Certificate Authority).

***Assumptions:***
 - CA's certificate must be installed to the JVM or machine's X509 Default TrustManager.
 - Client certificate authority name matches the temporal sever host name.

**1. Set environment variables**

```bash
# Environment variables
export TEMPORAL_CLIENT_CERT="</path/to/client.pem>"
export TEMPORAL_CLIENT_KEY="</path/to/client.key>"
export TEMPORAL_ENDPOINT="<temporal-host-name:port>"
export TEMPORAL_NAMESPACE="<namespace>"
```

**2. Start the Worker**

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.ssl.SslEnabledWorker
```

**3. Expected result**

```
[main] INFO  i.t.s.WorkflowServiceStubsImpl - Created WorkflowServiceStubs for channel: ManagedChannelOrphanWrapper{delegate=ManagedChannelImpl{logId=1, target=temporal-host-name:7233}} 
```

## SslEnabledWorkerCustomCA

This sample shows how to start a worker that connects to a temporal cluster with mTLS enabled; created by ([tls-simple sample](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple));

SslEnabledWorkerCustomCA demonstrates:
- Passing a custom CA certificate file as parameter
- Overriding the authority name used for TLS handshakes (if needed)

**1. Start a temporal cluster with tls**

```bash
# 1. clone the samples-server repository 
git clone https://github.com/temporalio/samples-server.git

# 2. change `tls/tls-simple/docker-compose.yml` 
#replace: "SKIP_DEFAULT_NAMESPACE_CREATION=true"
#with: "SKIP_DEFAULT_NAMESPACE_CREATION=false"

# 3. follow the readme to generate certificates and start temporal cluster
# https://github.com/temporalio/samples-server/tree/main/tls/tls-simple 
cd samples-server/tls/tls-simple

# generate certificates
./generate-test-certs.sh

# start temporal cluster
./start-temporal.sh
```

**2. Set environment variables**

```bash
# Environment variables
# paths to ca cert, client cert and client key come from the previous step 
export TEMPORAL_CLIENT_CERT="</path/to/client.pem>"
export TEMPORAL_CLIENT_KEY="</path/to/client.key>"
export TEMPORAL_CA_CERT="</path/to/ca.cert>"
export TEMPORAL_ENDPOINT="localhost:7233"
export TEMPORAL_NAMESPACE="default"
export TEMPORAL_SERVER_HOSTNAME="tls-sample"
```

**2. Start the Worker**

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.ssl.SslEnabledWorkerCustomCA
```

**3. Expected result**

```
[main] INFO  i.t.s.WorkflowServiceStubsImpl - Created WorkflowServiceStubs for channel: ManagedChannelOrphanWrapper{delegate=ManagedChannelImpl{logId=1, target=temporal-host-name:7233}} 
```

