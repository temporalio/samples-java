# Cadence Samples
These are some samples to demonstrate various capabilities of Java Cadence client and server.  You can learn more about cadence at:
* Cadence: https://github.com/uber/cadence
* Java Cadence Client: https://github.com/uber-java/cadence-client
* Go Cadence Client: https://github.com/uber-go/cadence-client

## Prerequisite
Run Cadence Server

See instructions for running the Cadence Server: https://github.com/uber/cadence/blob/master/README.md

## Build Samples

We are working on getting [cadence-client library](https://github.com/uber-java/cadence-client) into a public Maven repository.
In the meantime before running samples it has to be build to get it into the local maven cache.
See instructions from the Cadence Client README for the instructions.

After cadence-client library is available just run

    ./gradlew build`
    
to build the samples. Verify that they actually can run:

    ./gradlew execute -PmainClass=com.uber.cadence.samples.hello.HelloActivity

## Overview of the Samples

* **HelloWorld** -- obligatory single activity workflow.

* **PeriodicWorkflow** -- shows how to create a workflow that periodically executes an activity which name 
  and arguments are specified at runtime. 
  The workflow can run for extended periods and hence it uses the _continue as new execution_ feature.

* **FileProcessing** -- shows a workflow for media processing use case. The sample workflow
  downloads a file from an Amazon S3 bucket, creates a zip file and uploads that zip file back to
  S3. The sample uses the task routing feature.

* **SplitMerge** -- the workflow in this sample processes a large data set by splitting it up into
  smaller data sets. The sample calculates the average of a large set of numbers stored in a file in
  S3. The smaller data sets are assigned to workers and the results of processing are merged to
  produce the final result.
  
## Configuring Samples

If you are running local container the HelloWorld and PeriodicWorkflow samples do not need any additional configuration.

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

## Running the samples

Each sample has particular requirements for running it. Here's how to run each of the samples once
you've built them using the preceding instructions.

### Hello World

To run hello world:

    ./gradlew execute -PmainClass=com.uber.cadence.samples.hello.HelloActivity

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

    ./gradlew execute -PmainClass=com.uber.cadence.samples.fileprocessing.FileProcessingWorker
    
The second is responsible for starting workflows: 

    ./gradlew execute -PmainClass=com.uber.cadence.samples.fileprocessing.FileProcessingStarter
    
### Split Merge

The sample has two executables. You should run each command in a separate terminal window.

    ./gradlew execute -PmainClass=com.uber.cadence.samples.splitmerge.SplitMergeWorker
    ./gradlew execute -PmainClass=com.uber.cadence.samples.splitmerge.SplitMergeStarter

