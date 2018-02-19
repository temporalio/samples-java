An experimental port of SWF Flow Framework Samples to run on top of Cadence.

The package uses AWS Flow Framework from <https://github.com/mfateev/swf-flow-library-java>
which is not found in global maven repositories yet. 

# AWS Flow Framework Samples

These samples demonstrate how to use AWS Flow Framework. The following samples are included:

* **HelloWorld** -- this sample includes a very simple workflow that calls an activity to print
  hello world to the console. It shows the basic usage of AWS Flow Framework, including defining
  contracts, implementation of activities and workflow coordination logic and worker programs to
  host them.

* **HelloLambda** -- this sample shows how you can create a workflow that calls an AWS Lambda task
  instead of a traditional Amazon SWF activity.

* **Booking** -- shows an example workflow for making a reservation, including flight and rental
  car.

* **FileProcessing** -- shows a workflow for media processing use case. The sample workflow
  downloads a file from an Amazon S3 bucket, creates a zip file and uploads that zip file back to
  S3. The sample uses the task routing feature.

* **PeriodicWorkflow** -- shows how to create a workflow that periodically executes an activity. The
workflow can run for extended periods and hence it uses the continue as new execution feature.

* **SplitMerge** -- the workflow in this sample processes a large data set by splitting it up into
  smaller data sets. The sample calculates the average of a large set of numbers stored in a file in
  S3. The smaller data sets are assigned to workers and the results of processing are merged to
  produce the final result.

* **Deployment** -- the workflow in this sample shows deployment of interdependent components.

* **Cron** -- the workflow in this sample starts an activity periodically based on a cron schedule.

* **CronWithRetry** -- this is an enhanced version of the Cron sample that uses the exponential
  retry feature to retry the activity if it fails.
  
## Prerequisites

Cadence service running. See https://github.com/uber/cadence for service setup.

## Configuring and Building the samples

The steps for configuring and building the AWS Flow Framework for Java samples are:

1. Checkout and build the cadence branch of [swf-flow-library-java](https://github.com/mfateev/swf-flow-library-java) using mvn compile.
 Run Use mvn install to get it into local Maven cache.

2. Checkout and build the cadence branch [aws-swf-build-tools](https://github.com/mfateev/aws-swf-build-tools) using mvn compile.
Run Use mvn install to get it into local Maven cache.

3. Checkout and build the cadence branch of [swf-flow-library-samples-java](https://github.com/mfateev/swf-flow-library-samples-java)  using mvn compile.

4. Create the *Samples* domain

2. Open the `access.properties` file in the `samples` directory.

3. Update Cadence host and port values to a service API. Keep these values for local Cadence service:

        Cadence.host=127.0.0.1
        Cadence.port=7933

5. Save the `access.properties` file.

6. Set the environment variable `AWS_SWF_SAMPLES_CONFIG` to the full path of the directory
   containing the `access.properties` file.

    On Linux, Unix or OS X, use this command to set the environment variable:

        export AWS_SWF_SAMPLES_CONFIG=<Your SDK Directory>/src/samples/AwsFlowFramework

    On Windows run this command:

        set AWS_SWF_SAMPLES_CONFIG=<Your SDK Directory>/src/samples/AwsFlowFramework

## Running the samples

Each sample has particular requirements for running it. Here's how to run each of the samples once
you've built them using the preceding instructions.

### Hello World

To run hello world:

    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.helloworld.HelloWorld

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

    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.fileprocessing.FileProcessingWorker
    
The second is responsible for starting workflows: 

    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.fileprocessing.FileProcessingStarter
    
### Periodic Workflow

    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.periodicworkflow.PeriodicWorkflowWorker
    
    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.periodicworkflow.PeriodicWorkflowStarter

### Split Merge

The sample has two executables. You should run each command in a separate terminal window.

    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.splitmerge.SplitMergeWorker
    mvn exec:java -Dexec.mainClass=com.uber.cadence.samples.splitmerge.SplitMergeStarter

