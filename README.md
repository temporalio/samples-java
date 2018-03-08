# Cadence Samples
These are some samples to demonstrate various capabilities of Java Cadence client and server.  You can learn more about cadence at:
* Cadence: https://github.com/uber/cadence
* Java Cadence Client: https://github.com/uber-java/cadence-client
* Go Cadence Client: https://github.com/uber-go/cadence-client

## Overview of the Samples

* **HelloWorld Samples** 
  * _HelloActivity_ is a sample of a single activity workflow
  * _HelloActivityRetry_ demonstrates how to retry an activity
  * _HelloAsync_ is a sample of how to call activities asynchronously and wait for them using Promises.
  * _HelloAsyncLambda_ is a sample of how to run a part of a workflow asynchronously in a separate task (thread).
  * _HelloAsyncActivityCompletion_ is a sample of an asynchronous activity implementation.
  * _HelloChild_ is a sample of a child workflow
  * _HelloException_ demonstrates exception propagation and wrapping
  * HelloQuery is a sample of a query
  * HelloSignal is a sample of sending and handling a signal.
  * HelloPeriodic is a sample workflow that executes an activity periodically forever. 

* **FileProcessing** -- shows a workflow for media processing use case. The sample workflow
  downloads a file from an Amazon S3 bucket, creates a zip file and uploads that zip file back to
  S3. The sample uses the task routing feature. Requires AWS credentials.

* **SplitMerge** -- the workflow in this sample processes a large data set by splitting it up into
  smaller data sets. The sample calculates the average of a large set of numbers stored in a file in
  S3. The smaller data sets are assigned to workers and the results of processing are merged to
  produce the final result. Requires S3 credentials.
    
## Build Samples
  
  We are working on getting [cadence-client library](https://github.com/uber-java/cadence-client) into a public Maven repository.
  In the meantime before running samples it has to be build to get it into the local maven cache.
  See instructions from the Cadence Client README for the instructions.
  
  After cadence-client library is available just run
  
      ./gradlew build`
      
  to build the samples. Verify that they actually can run:
  
      ./gradlew -q execute -PmainClass=com.uber.cadence.samples.hello.HelloActivity
  

## Configuring Service and S3 Access Keys

If you are running local container the HelloWorld samples do not need any additional configuration.

The steps for configuring and building other samples for Java Cadence Client are:

1. Open the `access.properties` file in the `samples` directory.

2. Update Cadence host and port values to a service API. Keep these values for a local Cadence service:

        Cadence.host=127.0.0.1
        Cadence.port=7933

2. If planning to run samples that access S3 locate the following sections and fill in your Access Key ID and Secret Access Key.

        # Fill in your AWS Access Key ID and Secret Access Key for S3
        # http://aws.amazon.com/security-credentials
        S3.Access.ID=<Your AWS Access Key>
        S3.Secret.Key=<Your AWS Secret Key>
        S3.Account.ID=<Your AWS Account ID>


5. Save the `access.properties` file.

6. Set the environment variable `AWS_SWF_SAMPLES_CONFIG` to the full path of the directory
   containing the `access.properties` file.

    On Linux, Unix or OS X, use this command to set the environment variable:

        export AWS_SWF_SAMPLES_CONFIG=<Your SDK Directory>

    On Windows run this command:

        set AWS_SWF_SAMPLES_CONFIG=<Your SDK Directory>

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

