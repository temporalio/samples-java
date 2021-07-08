# Temporal Java SDK Samples

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples?ref=badge_shield)

This repository contains sample Workflow applications that demonstrate various capabilities of Temporal using the [Java SDK](https://github.com/temporalio/sdk-java).

- Temporal Server repo: https://github.com/temporalio/temporal
- Temporal Java SDK repo: https://github.com/temporalio/sdk-java
- Java SDK docs: https://docs.temporal.io/docs/java/introduction/

## Table of Contents

- [How to use](#how-to-use)
- [Temporal Web UI](temporal-web-ui)
- [Samples directory](#samples-directory)
    - [Hello samples](#Hello-samples)
    - [Scenario-based samples](#scenario-based-samples)
    - [API demonstrations](#api-demonstrations)
- [IDE Integration](#IDE-Integration)

## How to use

1. Clone this repository:

       git clone https://github.com/temporalio/samples-java
       cd samples-java

2. Build the examples and run tests:

       ./gradlew build

3. You need a locally running Temporal Server instance to run the samples. We recommend a locally running
   version of the Temporal Server managed via [Docker Compose](https://docs.docker.com/compose/gettingstarted/):

       git clone https://github.com/temporalio/docker-compose.git
       cd  docker-compose
       docker-compose up

Note that for the "listworkflows" example you need to have the Elasticsearch feature 
enabled on the Temporal Server side. To do this you can run locally with:

       git clone https://github.com/temporalio/docker-compose.git
       cd  docker-compose
       docker-compose -f docker-compose-cas-es.yml up

Alternatively you could install the Temporal Server on Kubernetes / Minicube using the [Temporal Helm charts](https://github.com/temporalio/helm-charts).
Note that in this case you should use the [Temporal CLI (tctl)](https://docs.temporal.io/docs/system-tools/tctl/) tool to create a namespace called "default":

       tctl --ns default n re

## Temporal Web UI

The Temporal Server running in a docker container includes a Web UI.

Connect to [http://localhost:8088](http://localhost:8088).

If you have deployed the Temporal Server on Kubernetes using Helm Charts, you can use the kubectl command-line tool
to forward your local machine ports to the Temporal Web UI:

        kubectl port-forward services/temporaltest-web 8088:8088
        kubectl port-forward services/temporaltest-frontend-headless 7233:7233

With this you should be able to access the Temporal Web UI with [http://localhost:8088](http://localhost:8088).

## Samples directory

The following section lists all available samples.
Click on the sample link to view the README, which contains instructions on how to run them.

Each sample has an associated unit test which demonstrates the use of the Temporal Java SDK testing API.
All tests are available under [src/test/java](https://github.com/temporalio/samples-java/tree/master/src/test/java/io/temporal/samples)

<!-- @@@SNIPSTART samples-java-readme-samples-directory -->

### Hello samples

- [**Hello**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/hello): This sample includes a number of individual Workflows that can be executed independently. Each one demonstrates something specific.
  - [**HelloActivity**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivity.java): Demonstrates a Workflow Definition that executes a single Activity.
  - [**HelloActivityRetry**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivityRetry.java): Demonstrates how to Retry an Activity Execution.
  - [**HelloActivityExclusiveChoice**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivityExclusiveChoice.java): Demonstrates how to execute Activities based on dynamic input.
  - [**HelloAsync**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsync.java): Demonstrates how to execute Activities asynchronously and wait for them using Promises.
  - [**HelloParallelActivity**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloParallelActivity.java): Demonstrates how to execute multiple Activities in parallel, asynchronously, and wait for them using `Promise.allOf`.
  - [**HelloAsyncActivityCompletion**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsyncActivityCompletion.java): Demonstrates how to complete an Activity Execution asynchronously.
  - [**HelloAsyncLambda**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsyncLambda.java): Demonstrates how to execute part of a Workflow asynchronously in a separate task (thread).
  - [**HelloCancellationScope**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloCancellationScope.java): Demonstrates how to explicitly cancel parts of a Workflow Execution.
  - [**HelloDetachedCancellationScope**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloDetachedCancellationScope.java): Demonstrates how to execute cleanup code after a Workflow Execution has been explicitly cancelled.
  - [**HelloChild**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloChild.java): Demonstrates how to execute a simple Child Workflow.
  - [**HelloCron**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloCron.java): Demonstrates how to execute a Workflow according to a cron schedule.
  - [**HelloDynamic**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloDynamic.java): Demonstrates how to use `DynamicWorkflow` and `DynamicActivity` interfaces.
  - [**HelloPeriodic**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloPeriodic.java): Demonstrates the use of the Continue-As-New feature.
  - [**HelloException**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloException.java): Demonstrates how to handle exception propagation and wrapping.
  - [**HelloLocalActivity**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloLocalActivity.java): Demonstrates the use of a [Local Activity](https://docs.temporal.io/docs/jargon/mesh/#local-activity).
  - [**HelloPolymorphicActivity**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloPolymorphicActivity.java): Demonstrates Activity Definitions that extend a common interface.
  - [**HelloQuery**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloQuery.java): Demonstrates how to Query the state of a Workflow Execution.
  - [**HelloSignal**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSignal.java): Demonstrates how to send and handle a Signal.
  - [**HelloSaga**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSaga.java): Demonstrates how to use the SAGA feature.
  - [**HelloSearchAttributes**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSearchAttributes.java): Demonstrates how to add custom Search Attributes to Workflow Executions.
  - [**HelloSideEffect**](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSideEffect.java)**: Demonstrates how to implement a Side Effect.

### Scenario-based samples

- [**File Processing Sample**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/fileprocessing): Demonstrates how to route tasks to specific Workers. This sample has a set of Activities that download a file, processes it, and uploads the result to a destination. Any Worker can execute the first Activity. However, the second and third Activities must be executed on the same host as the first one.

- [**Booking SAGA**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/bookingsaga): Demonstrates Temporals take on the Camunda BPMN "trip booking" example.

- [**Money Transfer**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/moneytransfer): Demonstrates the use of a dedicated Activity Worker.

- [**Money Batch**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/moneybatch): Demonstrates a situation where a single deposit should be initiated for multiple withdrawals. For example, a seller might want to be paid once per fixed number of transactions. This sample can be easily extended to perform a payment based on more complex criteria, such as at a specific time or an accumulated amount. The sample also demonstrates how to Signal the Workflow when it executes (*Signal with start*). If the Workflow is already executing, it just receives the Signal. If it is not executing, then the Workflow executes first, and then the Signal is delivered to it. *Signal with start* is a "lazy" way to execute Workflows when Signaling them.

### API demonstrations

- [**Updatable Timer**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/updatabletimer): Demonstrates the use of a helper class which relies on `Workflow.await` to implement a blocking sleep that can be updated at any moment.

- [**Workflow Interceptor**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/interceptor): Demonstrates how to create and register a simple Workflow Interceptor.

- [**List Workflows**](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/listworkflows): Demonstrates the use of custom search attributes and ListWorkflowExecutionsRequest with custom queries.

<!-- @@@SNIPEND -->

## IDE Integration

### IntelliJ

It is possible to run the samples from the command line, but if you prefer IntelliJ here are the import steps:

* Navigate to **File**->**New**->**Project from Existing Sources**.
* Select the cloned directory.
* In the **Import Project page**, select **Import project from external model**
* Choose **Gradle** and then click **Next**
* Click **Finish**.
