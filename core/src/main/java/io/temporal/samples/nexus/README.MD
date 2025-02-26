# Nexus

Temporal Nexus is a new feature of the Temporal platform designed to connect durable executions across team, namespace,
region, and cloud boundaries. It promotes a more modular architecture for sharing a subset of your team’s capabilities
via well-defined service API contracts for other teams to use, that abstract underlying Temporal primitives, like
Workflows, or execute arbitrary code.

Learn more at [temporal.io/nexus](https://temporal.io/nexus).

This sample shows how to use Temporal for authoring a Nexus service and call it from a workflow.

### Sample directory structure

- [service](./service) - shared service definition
- [caller](./caller) - caller workflows, worker, and starter
- [handler](./handler) - handler workflow, operations, and worker
- [options](./options) - command line argument parsing utility

## Getting started locally

### Get `temporal` CLI to enable local development

1. Follow the instructions on the [docs
   site](https://learn.temporal.io/getting_started/go/dev_environment/#set-up-a-local-temporal-service-for-development-with-temporal-cli)
   to install Temporal CLI.

> NOTE: The recommended version is at least v1.3.0.

### Spin up environment

#### Start temporal server

> HTTP port is required for Nexus communications

```
temporal server start-dev
```

### Initialize environment

In a separate terminal window

#### Create caller and target namespaces

```
temporal operator namespace create --namespace my-target-namespace
temporal operator namespace create --namespace my-caller-namespace
```

#### Create Nexus endpoint

```
temporal operator nexus endpoint create \
  --name my-nexus-endpoint-name \
  --target-namespace my-target-namespace \
  --target-task-queue my-handler-task-queue \
  --description-file ./core/src/main/java/io/temporal/samples/nexus/service/description.md
```

## Getting started with a self-hosted service or Temporal Cloud

Nexus is currently available as
[Public Preview](https://docs.temporal.io/evaluate/development-production-features/release-stages).

Self hosted users can [try Nexus
out](https://github.com/temporalio/temporal/blob/main/docs/architecture/nexus.md#trying-nexus-out) in single cluster
deployments with server version 1.25.0.

### Make Nexus calls across namespace boundaries

> Instructions apply for local development, for Temporal Cloud or a self-hosted setups, supply the relevant [CLI
> flags](./options/ClientOptions.java) to properly set up the connection.

In separate terminal windows:

### Nexus handler worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexus.handler.HandlerWorker \
    --args="-target-host localhost:7233 -namespace my-target-namespace"
```

### Nexus caller worker

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexus.caller.CallerWorker \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Start caller workflow

```
./gradlew -q execute -PmainClass=io.temporal.samples.nexus.caller.CallerStarter \
    --args="-target-host localhost:7233 -namespace my-caller-namespace"
```

### Output

which should result in:
```
[main] INFO  i.t.s.nexus.caller.CallerStarter - Workflow result: Nexus Echo 👋 
[main] INFO  i.t.s.nexus.caller.CallerStarter - Workflow result: ¡Hola! Nexus 👋  
```
