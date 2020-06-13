# Java Temporal Samples
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples?ref=badge_shield)

These samples demonstrate various capabilities of Java Temporal client and server. You can learn more about Temporal at:
* [Temporal Service](https://github.com/temporalio/temporal)
* [Temporal Java Client](https://github.com/temporalio/temporal-java-sdk)
* [Go Temporal Client](https://github.com/temporalio/temporal-go-sdk)

## Overview of the Samples

* **HelloWorld Samples**

    The following samples demonstrate:

  * **HelloActivity**: a single activity workflow
  * **HelloActivityRetry**: how to retry an activity
  * **HelloAsync**: how to call activities asynchronously and wait for them using Promises
  * **HelloAsyncLambda**: how to run part of a workflow asynchronously in a separate task (thread)
  * **HelloAsyncActivityCompletion**: an asynchronous activity implementation
  * **HelloChild**: a child workflow
  * **HelloException**: exception propagation and wrapping
  * **HelloQuery**: a query
  * **HelloSignal**: sending and handling a signal
  * **HelloPeriodic**: a sample workflow that executes an activity periodically forever

* **FileProcessing** demonstrates task routing features. The sample workflow downloads a file, processes it, and uploads
    the result to a destination. The first activity can be picked up by any worker. However, the second and third activities
    must be executed on the same host as the first one.

## Get the Samples

Run the following commands:

     git clone https://github.com/temporalio/temporal-java-samples
     cd temporal-java-samples

## Import into IntelliJ

In the IntelliJ user interface, navigate to **File**->**New**->**Project from Existing Sources**.

Select the cloned directory. In the **Import Project page**, select **Import project from external model**,
choose **Gradle** and then click **Next**->**Finish**.

## Build the Samples

      ./gradlew build

## Run Temporal Server

Run Temporal Server using Docker Compose:

     curl -L https://github.com/temporalio/temporal/releases/download/v0.23.1/docker.tar.gz | tar -xz --strip-components 1 docker/docker-compose.yml
     docker-compose up

If this does not work, see the instructions for running Temporal Server at https://github.com/temporalio/temporal/blob/master/README.md.

## See Temporal UI (Not Available yet!)

The Temporal Server running in a docker container includes a Web UI.

Connect to [http://localhost:8088](http://localhost:8088).

Enter the *sample* domain. You'll see a "No Results" page. After running any sample, change the 
filter in the
top right corner from "Open" to "Closed" to see the list of the completed workflows.

Click on a *RUN ID* of a workflow to see more details about it. Try different view formats to get a different level
of details about the execution history.

## Install Temporal CLI

[Command Line Interface Documentation](https://docs.temporal.io/docs/08_running_temporal/02_cli)

## Run the samples

Each sample has specific requirements for running it. The following sections contain information about
how to run each of the samples after you've built them using the preceding instructions.

Don't forget to check unit tests found under src/test/java!

### Hello World

To run the hello world samples:

    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivity
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityRetry
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsync
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsyncActivityCompletion
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsyncLambda
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloChild
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloException
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloPeriodic
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloCron
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloQuery
    ./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSignal

### File Processing

This sample has two executables. Execute each command in a separate terminal window. The first command
runs the worker that hosts the workflow and activities implementation. To demonstrate that activities
execute together, we recommend that you run more than one instance of this worker.

    ./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingWorker

The second command starts workflows. Each invocation starts a new workflow execution.

    ./gradlew -q execute -PmainClass=io.temporal.samples.fileprocessing.FileProcessingStarter
    
### Trip Booking

Temporal implementation of the [Camunda BPMN trip booking example](https://github.com/berndruecker/trip-booking-saga-java)

Demonstrates Temporal approach to SAGA.

To run:

    ./gradlew -q execute -PmainClass=io.temporal.samples.bookingsaga.TripBookingSaga
    
The produced exception trace is part of the sample, so don't get confused by it.

### Notes for MacOSX Users
Due to issues with default hostname resolution (see https://stackoverflow.com/questions/33289695/inetaddress-getlocalhost-slow-to-run-30-seconds), MacOSX Users may see gRPC DEADLINE_EXCEEDED errors in normal operation.

This can be solved by adding the following entries to your `/etc/hosts` file (where my-macbook is your hostname):

```conf
127.0.0.1   my-macbook
::1         my-macbook
```


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Ftemporalio%2Ftemporal-java-samples?ref=badge_large)