# Temporal Java SDK Samples

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples?ref=badge_shield)

This repository contains sample Workflow applications that demonstrate various capabilities of Temporal using the [Java SDK](https://github.com/temporalio/sdk-java).

- Temporal Server repo: https://github.com/temporalio/temporal
- Temporal Java SDK repo: https://github.com/temporalio/sdk-java
- Java SDK docs: https://docs.temporal.io/docs/java/introduction/

## Table of Contents

- [How to use](#How-to-use)
- [Temporal Web UI](#Temporal-Web-UI)
- [Run the Samples](#Run-the-Samples)
    - [Hello World Samples](#Hello-World-Samples)
    - [File Processing Sample](#File-Processing-Sample)
    - [Booking SAGA Sample](#Booking-SAGA-Sample)
    - [Money Transfer Sample](#Money-Transfer-Sample)
    - [Money Batch Sample](#Money-Batch-Sample)
    - [Updatable Timer Sample](#Updatable-Timer-Sample)
    - [Workflow Interceptor Sample](#Workflow-Interceptor-Sample)
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

## Run the Samples

The following sections describe all available samples and how to run them.

Each sample has an associated unit test. You should definitely check these out as they demonstrate
the Temporal Java SDK testing API. 

All tests are available under [src/test/java](https://github.com/temporalio/samples-java/tree/master/src/test/java/io/temporal/samples)

### Hello World Samples

Each Hello World sample  demonstrates one feature of the SDK in a single file. Note that single file format is
used for sample brevity and is not something we recommend for real applications.

  * **[HelloActivity](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivity.java)**: Single activity workflow
  * **[HelloActivityRetry](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivityRetry.java)**: How to retry an activity
  * **[HelloActivityExclusiveChoice](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivityExclusiveChoice.java)**: How to execute activities based on dynamic input
  * **[HelloAsync](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsync.java)**: How to call activities asynchronously and wait for them using Promises
  * **[HelloParallelActivity](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloParallelActivity.java)**: How to call multiple parallel activities asynchronously and wait for them using Promise.allOf
  * **[HelloAsyncActivityCompletion](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsyncActivityCompletion.java)**: Asynchronous activity implementation
  * **[HelloAsyncLambda](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsyncLambda.java)**: How to run part of a workflow asynchronously in a separate task (thread)
  * **[HelloCancellationScope](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloCancellationScope.java)**: How to explicitly cancel parts of a workflow
  * **[HelloChild](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloChild.java)**: Child workflow
  * **[HelloCron](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloCron.java)**: Workflow that is executed according to a cron schedule
  * **[HelloPeriodic](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloPeriodic.java)**: Workflow that executes some logic periodically 
  * **[HelloException](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloException.java)**: Exception propagation and wrapping
  * **[HelloLocalActivity](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloLocalActivity.java)**: Workflow with a [local activity](https://docs.temporal.io/docs/concept-activities/#local-activities)
  * **[HelloPolymorphicActivity](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloPolymorphicActivity.java)**: Activities that extend a common interface
  * **[HelloQuery](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloQuery.java)**: Demonstrates how to query a state of a single workflow
  * **[HelloSignal](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSignal.java)**: Sending and handling a signal
  * **[HelloSaga](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSaga.java)**: SAGA pattern support
  * **[HelloSearchAttributes](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSearchAttributes.java)**: Custom search attributes that can be used to find workflows using predicates
  * **[HelloSideEffect](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloSideEffect.java)**: Demonstrates the use of workflow SideEffect

####  Running Hello World Samples

  To run each hello world sample, use one of the following commands:

      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivity
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityRetry
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityExclusiveChoice
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsync
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloParallelActivity
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsyncActivityCompletion
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsyncLambda
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloCancellationScope
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloChild
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloCron
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloException
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloLocalActivity
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloPeriodic
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloPolymorphicActivity
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloQuery
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSaga
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSignal
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSearchAttributes
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSideEffect

### File Processing Sample

[FileProcessing](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/fileprocessing)
demonstrates task routing features. 
The sample workflow downloads a file, processes it, and uploads the result to a destination. Any worker can pick up the first activity. However, the second and third activity must be executed on the same host as the first one.

####  Running the File Processing Sample

The sample has two executables. Execute each command in a separate terminal window. The first command
runs the worker that hosts the workflow and activities implementation. To demonstrate that activities
execute together, we recommend running more than one instance of this worker.

    ./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingWorker

The second command starts workflows. Each invocation starts a new workflow execution.

    ./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingStarter

### Booking SAGA Sample

[Booking SAGA](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/bookingsaga)
is a Temporal take on Camunda BPMN trip booking example.

####  Running the Booking Saga Sample

    ./gradlew -q execute -PmainClass=io.temporal.samples.bookingsaga.TripBookingSaga

### Money Transfer Sample

Basic [Money Transfer](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/moneytransfer) example.

####  Running the Money Transfer Sample

Money Transfer sample has three separate processes. One to host workflow code,
another activity, and the third one to request transfers.

Start workflow worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.AccountTransferWorker

Start activity worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.AccountActivityWorker

Execute once per requested transfer:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.TransferRequester

### Money Batch Sample

[The sample](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/moneybatch)
demonstrates a situation when a single deposit should be initiated for multiple withdrawals.
For example, a seller might want to be paid once per fixed number of transactions.
The sample can be easily extended to perform a payment based on more complex criteria like a specific time
or accumulated amount.

The sample also demonstrates the *signal with start* way of starting workflows. If the workflow is already running, it
just receives the signal. If it is not running, then it is started first, and then the signal is delivered to it.
You can think about *signal with start* as a lazy way to create workflows when signaling them.

####  Running the Money Batch Sample

Money Batch sample has three separate processes. One to host workflow code,
another activity, and the third one to request transfers.

Start workflow worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.AccountTransferWorker

Start activity worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.AccountActivityWorker

Execute at least three times to request three transfers (example batch size):

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.TransferRequester

### Updatable Timer Sample

The [Updatable Timer](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/updatabletimer) sample
demonstrates a helper class which relies on Workflow.await to implement a blocking sleep that can be updated at any moment.

####  Running the Updatable Timer Sample

Update Timer sample has three separate processes. One to host workflow code,
another to start workflow execution, and the third one to send signals to request timer updates.

Start workflow worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker

Start workflow execution:

    ./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.DynamicSleepWorkflowStarter

Extend timer duration:

    ./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.WakeUpTimeUpdater

### Workflow Interceptor Sample

The [Workflow Interceptor](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/interceptor) sample
demonstrates how to create and register a simple Workflow Interceptor.

####  Running the Workflow Interceptor Sample

Run the starter:

    ./gradlew -q execute -PmainClass=io.temporal.samples.interceptor.InterceptorStarter

### IDE Integration

#### IntelliJ

It is possible to run the samples from the command line, but if you prefer IntelliJ here are the import steps:

* Navigate to **File**->**New**->**Project from Existing Sources**.
* Select the cloned directory.
* In the **Import Project page**, select **Import project from external model**
* Choose **Gradle** and then click **Next**
* Click **Finish**.
