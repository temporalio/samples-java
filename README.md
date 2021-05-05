# Java Temporal Samples
These samples demonstrate various capabilities of Java Temporal client and server. You can learn more about Temporal at:
* [temporal.io](https://temporal.io)
* [Temporal Service](https://github.com/temporalio/temporal)
* [Temporal Java SDK](https://github.com/temporalio/sdk-java)
* [Temporal Go SDK](https://github.com/temporalio/sdk-go)

## Setup

### macOS Specific
Due to issues with default hostname resolution
(see [this StackOverflow question](https://stackoverflow.com/questions/33289695/inetaddress-getlocalhost-slow-to-run-30-seconds) for more details),
macOS Users may see gRPC `DEADLINE_EXCEEDED` errors when running the samples or any other gRPC related code.

To solve the problem add the following entries to your `/etc/hosts` file (where my-macbook is your hostname):

```conf
127.0.0.1   my-macbook
::1         my-macbook
```

### Get the Samples

Run the following commands:

     git clone https://github.com/temporalio/samples-java
     cd samples-java

### Build the Samples

      ./gradlew build

### Import into IntelliJ

It is possible to run the samples from the command line, but if you prefer the IntelliJ here are the import steps:

* Navigate to **File**->**New**->**Project from Existing Sources**.
* Select the cloned directory.
* In the **Import Project page**, select **Import project from external model**
* Choose **Gradle** and then click **Next**
* Click **Finish**.

### Run Temporal Server

To run the examples a running Temporal service is required. We recommend a locally running
version of the Temporal Server managed via [Docker Compose](https://docs.docker.com/compose/gettingstarted/).
In order to set this up, follow instructions below:


Samples require Temporal service to run. We recommend a locally running version of Temporal Server
managed through [Docker Compose](https://docs.docker.com/compose/gettingstarted/):

     git clone https://github.com/temporalio/docker-compose.git
     cd  docker-compose
     docker-compose up

This will start the Temporal service locally and allow you to execute the samples.

## See Temporal UI

The Temporal Server running in a docker container includes a Web UI.

Connect to [http://localhost:8088](http://localhost:8088).

Click on a *RUN ID* of a workflow to see more details about it. Try different view formats to get a different level
of details about the execution history.

## Install Temporal CLI (tctl)

[Command Line Interface Documentation](https://docs.temporal.io/docs/system-tools/tctl)

## Samples

Each sample has specific requirements for running it. The following sections contain information about
how to run each of the samples after you've built them using the preceding instructions.

Don't forget to check unit tests found under [src/test/java](https://github.com/temporalio/samples-java/tree/master/src/test/java/io/temporal/samples)!

### HelloWorld

Each Hello World sample  demonstrates one feature of the SDK in a single file. Note that single file format is
used for sample brevity and is not something we recommend for real applications.

  * **[HelloActivity](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivity.java)**: Single activity workflow
  * **[HelloActivityRetry](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivityRetry.java)**: How to retry an activity
  * **[HelloActivityExclusiveChoice](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloActivityExclusiveChoice.java)**: How to execute activities based on dynamic input
  * **[HelloAsync](https://github.com/temporalio/samples-java/blob/master/src/main/java/io/temporal/samples/hello/HelloAsync.java)**: How to call activities asynchronously and wait for them using Promises
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


  To run the hello world samples:

      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivity
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityRetry
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityExclusiveChoice
      ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsync
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

### File Processing
[FileProcessing](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/fileprocessing)
demonstrates task routing features. The sample workflow downloads a file, processes it, and uploads the result to a destination. Any worker can pick up the first activity. However, the second and third activity must be executed on the same host as the first one.

The sample has two executables. Execute each command in a separate terminal window. The first command
runs the worker that hosts the workflow and activities implementation. To demonstrate that activities
execute together, we recommend running more than one instance of this worker.

    ./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingWorker

The second command starts workflows. Each invocation starts a new workflow execution.

    ./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingStarter

### Booking SAGA

[Booking SAGA](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/bookingsaga)
is a Temporal take on Camunda BPMN trip booking example.

To run:

    ./gradlew -q execute -PmainClass=io.temporal.samples.bookingsaga.TripBookingSaga

### Money Transfer

Basic [Money Transfer](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/moneytransfer) example.

Money Transfer example has three separate processes. One to host workflow code,
another activity, and the third one to request transfers.

Start workflow worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.AccountTransferWorker

Start activity worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.AccountActivityWorker

Execute once per requested transfer:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneytransfer.TransferRequester

### Money Batch

[The sample](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/moneybatch)
demonstrates a situation when a single deposit should be initiated for multiple withdrawals.
For example, a seller might want to be paid once per fixed number of transactions.
The sample can be easily extended to perform a payment based on more complex criteria like a specific time
or accumulated amount.

The sample also demonstrates *signal with start* way of starting workflows. If the workflow is already running, it
just receives the signal. If it is not running, then it is started first, and then the signal is delivered to it.
You can think about *signal with start* as a lazy way to create workflows when signaling them.

Money Batch example has three separate processes. One to host workflow code,
another activity, and the third one to request transfers.

Start workflow worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.AccountTransferWorker

Start activity worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.AccountActivityWorker

Execute at least three times to request three transfers (example batch size):

    ./gradlew -q execute -PmainClass=io.temporal.samples.moneybatch.TransferRequester

### Updatable Timer

The [Updatable Timer](https://github.com/temporalio/samples-java/tree/master/src/main/java/io/temporal/samples/updatabletimer) sample
demonstrates a helper class which relies on Workflow.await to implement a blocking sleep that can be updated at any moment.

Money Batch example has three separate processes. One to host workflow code,
another to start workflow execution, and the third one to send signals to request timer updates.

Start workflow worker:

    ./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker

Start workflow execution:

    ./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.DynamicSleepWorkflowStarter

Extend timer duration:

    ./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.WakeUpTimeUpdater
