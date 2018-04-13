**If you work for Uber** use the internal _cadence-java-client-samples_ repository.
# Java Cadence Samples
These samples demonstrate various capabilities of Java Cadence client and server. You can learn more about Cadence at:
* [Cadence Service](https://github.com/uber/cadence)
* [Cadence Java Client](https://github.com/uber-java/cadence-client)
* [Go Cadence Client](https://github.com/uber-go/cadence-client)

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

      git clone https://github.com/mfateev/uber-java-cadence-samples
      cd cadence-java-client-samples

## Import into IntelliJ

In the IntelliJ user interface, navigate to **File**->**New**->**Project from Existing Sources**.

Select the cloned directory. In the **Import Project page**, select **Import project from external model**,
choose **Gradle** and then click **Next**->**Finish**.

## Build the Samples

Assumng that the https://github.com/uber-java/cadence-client is in local maven cache.

      ./gradlew build

If the latest samples stop building after you pull the latest version, refresh the Gradle dependencies:

      ./gradlew build --refresh-dependencies

Or, in IntelliJ, in the **Gradle projects** window, right-click "cadence-samples" and then click
**Refresh dependencies**.

## Run Cadence Server

Run Cadence Server using Docker Compose:

    curl -O https://raw.githubusercontent.com/uber/cadence/master/docker/docker-compose.yml
    docker-compose up

If this does not work, see the instructions for running Cadence Server at https://github.com/uber/cadence/blob/master/README.md.

## Register the Domain

To register the *sample* domain, run the following command once before running any samples:

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.common.RegisterDomain

## See Cadence UI

The Cadence Server running in a docker container includes a Web UI.

Connect to [http://localhost:8088](http://localhost:8088).

Enter the *sample* domain. You'll see a "No Results" page. After running any sample, change the 
filter in the
top right corner from "Open" to "Closed" to see the list of the completed workflows.

Click on a *RUN ID* of a workflow to see more details about it. Try different view formats to get a different level
of details about the execution history.

## Install Cadence CLI

Check out [Cadence Service](https://github.com/uber/cadence) and run **make bins**.
CLI executable named **cadence** will be in the root directory.

To not pass the domain on every command set the following environment variable:

    export CADENCE_CLI_DOMAIN=sample

After the CADENCE_CLI_DOMAIN is set, to list closed workflow executions run:

    cadence workflow list

To see a history of a particular workflow execution run:

    cadence workflow showid <WorkflowID>

To see a stack trace of a currently running workflow execution run:

    ./cadence workflow stack -w <WorkflowID>

## Run the samples

Each sample has specific requirements for running it. The following sections contain information about
how to run each of the samples after you've built them using the preceding instructions.

### Hello World

To run the hello world samples:

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloActivity
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloActivityRetry
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloAsync
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloAsyncActivityCompletion
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloAsyncLambda
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloChild
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloException
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloPeriodic
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloQuery
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloSignal

### File Processing

This sample has two executables. Execute each command in a separate terminal window. The first command
runs the worker that hosts the workflow and activities implementation. To demonstrate that activities
execute together, we recommend that you run more than one instance of this worker.

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.fileprocessing.FileProcessingWorker

The second command starts workflows. Each invocation starts a new workflow execution.

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.fileprocessing.FileProcessingStarter

