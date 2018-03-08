# Cadence Samples
These are some samples to demonstrate various capabilities of Java Cadence client and server.  You can learn more about cadence at:
* Cadence: https://github.com/uber/cadence
* Java Cadence Client: https://github.com/uber-java/cadence-client
* Go Cadence Client: https://github.com/uber-go/cadence-client

## Overview of the Samples

* **HelloWorld Samples** 
  * HelloActivity is a sample of a single activity workflow
  * HelloActivityRetry demonstrates how to retry an activity
  * HelloAsync is a sample of how to call activities asynchronously and wait for them using Promises.
  * HelloAsyncLambda is a sample of how to run a part of a workflow asynchronously in a separate task (thread).
  * HelloAsyncActivityCompletion is a sample of an asynchronous activity implementation.
  * HelloChild is a sample of a child workflow
  * HelloException demonstrates exception propagation and wrapping
  * HelloQuery is a sample of a query
  * HelloSignal is a sample of sending and handling a signal.
  * HelloPeriodic is a sample workflow that executes an activity periodically forever. 

* **FileProcessing** -- shows a workflow for media processing use case. The sample workflow
  downloads a file, processes it and uploads result to a destination. Demonstrates how to route activities to a 
  specific host.

## Build Samples
  
  We are working on getting [cadence-client library](https://github.com/uber-java/cadence-client) into a public Maven repository.
  In the meantime before running samples it has to be build to get it into the local maven cache.
  See instructions from the Cadence Client README for the instructions.
  
  After cadence-client library is available just run
  
      ./gradlew build`
      
  to build the samples. Verify that they actually can run:
  
      ./gradlew -q execute -PmainClass=com.uber.cadence.samples.common.RegisterDomain
  
## Prerequisite
  Run Cadence Server using Docker Compose

    curl -O https://raw.githubusercontent.com/uber/cadence/master/docker/docker-compose.yml
    docker-compose up
     
  If it does not work see instructions for running the Cadence Server at https://github.com/uber/cadence/blob/master/README.md

## Registering Domain

Run it once before running any samples to register domain.

./gradlew -q execute -PmainClass=com.uber.cadence.samples.common.RegisterDomain

## Running the samples

Each sample has particular requirements for running it. Here's how to run each of the samples once
you've built them using the preceding instructions.

### Hello World

To run hello world:

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

The *FileProcessing* sample uploads files to [Amazon S3](http://aws.amazon.com/s3/). To run this
sample, you will need to first [create an S3
bucket](http://docs.aws.amazon.com/AmazonS3/latest/gsg/CreatingABucket.html).

Then, locate the following section in the `access.properties` file and fill in the name of an S3
bucket that you want the sample to use:

        ####### FileProcessing Sample Config Values ##########
        Workflow.Input.TargetBucketName=<Your S3 bucket name>

The sample has two executables. You should run each command in a separate terminal window. The first one 
is the worker that hosts workflow and activities implementation:

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.fileprocessing.FileProcessingWorker
    
The second is responsible for starting workflows: 

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.fileprocessing.FileProcessingStarter
    
### Split Merge

The sample has two executables. You should run each command in a separate terminal window.

    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.splitmerge.SplitMergeWorker
    ./gradlew -q execute -PmainClass=com.uber.cadence.samples.splitmerge.SplitMergeStarter

