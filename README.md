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

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=HelloWorldWorker
    mvn exec:java -Dexec.mainClass=com.amazonaws.services.simpleworkflow.flow.examples.helloworld.WorkflowHost
    mvn exec:java -Dexec.mainClass=HelloWorldStarter

### Hello Lambda

The *HelloLambda* sample requires the use of an [AWS Lambda](http://aws.amazon.com/lambda/)
function. You can create the function in any way supported by AWS Lambda, including by using the AWS
Eclipse Toolkit.

**Note**: Using Lambda can incur costs to your AWS account. For more information, see the [AWS
Lambda Pricing page](http://aws.amazon.com/lambda/pricing/).

Visit the following pages to learn how to create AWS Lambda functions:

* [in Java using Eclipse](http://docs.aws.amazon.com/AWSToolkitEclipse/latest/GettingStartedGuide/lambda-tutorial.html).

* [in Java without Eclipse](http://docs.aws.amazon.com/lambda/latest/dg/java-lambda.html).

* [in Node.js](http://docs.aws.amazon.com/lambda/latest/dg/authoring-function-in-nodejs.html).

When you create the function, be sure that it exists in the same AWS region that you will run the
*HelloLambda* sample in. You will also need to provide Amazon SWF with access to your Lambda
function in order to run it using the sample.

**To create a Lambda role for SWF:**

1. Open the [Amazon IAM console](https://console.aws.amazon.com/iam/)

2. Click **Roles**, then **Create New Role**.

3. Give your role a name, such as `swf-lambda' and click **Next Step**.

4. Under **AWS Service Roles**, choose **AWS SWF**, and click **Next Step**.

5. Choose **AWSLambdaRole** from the list, click **Next Step** and then **Create Role** once you've
   reviewed the role.

    Once you have a Lambda function to run and an IAM policy that gives SWF access to it, then find the
    following section in the `access.properties` file and provide it with information about the function
    and role that you created:

        ####### HelloLambda Sample Config Values ######
        SWF.LambdaRole.ARN=<Your IAM role that authorizes SWF to invoke Lambda functions>
        SWFLambdaFunction.Name=<The name of your Lambda function>
        SWFLambdaFunction.Input=<Input for your Lambda function>

The *HelloLambda* sample uses an AWS Lambda task instead of running an activity, so you only need to
run the workflow host and workflow starter. Run each command in a separate terminal window from the
`samples` directory.

    mvn exec:java -Dexec.mainClass=com.amazonaws.services.simpleworkflow.flow.examples.hellolambda.WorkflowHost
    mvn exec:java -Dexec.mainClass=com.amazonaws.services.simpleworkflow.flow.examples.hellolambda.WorkflowExecutionStarter

### Booking

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=ActivityHost
    mvn exec:java -Dexec.mainClass=WorkflowHost
    mvn exec:java -Dexec.mainClass=WorkflowExecutionStarter

### Cron

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=ActivityHost
    mvn exec:java -Dexec.mainClass=WorkflowHost
    mvn exec:java -Dexec.mainClass=CronWorkflowExecutionStarter -Dexec.args="\"*/10 * * * * *\" PST 60"

The workflow starter takes three command line arguments that *must* be specified:

1. CRON_PATTERN: specifies the pattern used to determine the cron schedule for the periodic activity
   task. The above command specifies the pattern `*/10 * * * * *` to run the task every 10 seconds.

2. TIME_ZONE: specifies the time zone to use for time calculations. The above command specifies PST
   (Pacific Standard Time).

3. CONTINUE_AS_NEW_AFTER_SECONDS: specifies the duration, in seconds, after which the current
   execution should be closed and continued as a new execution. The above command specifies 60 seconds.

### Cron With Retry

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=ActivityHost
    mvn exec:java -Dexec.mainClass=WorkflowHost
    mvn exec:java -Dexec.mainClass=CronWithRetryWorkflowExecutionStarter" -Dmain-args="\"*/10 * * * * *\" PST 60

The workflow starter takes three command line arguments that *must* be specified:

1. CRON_PATTERN: specifies the pattern used to determine the cron schedule for the periodic activity
   task. The above command specifies the pattern `*/10 * * * * *` to run the task every 10 seconds.

2. TIME_ZONE: specifies the time zone to use for time calculations. The above command specifies PST
(Pacific Standard Time).

3. CONTINUE_AS_NEW_AFTER_SECONDS: specifies the duration, in seconds, after which the current
   execution should be closed and continued as a new execution. The above command specifies 60
   seconds.

### File Processing

The *FileProcessing* sample uploads files to [Amazon S3](http://aws.amazon.com/s3/). To run this
sample, you will need to first [create an S3
bucket](http://docs.aws.amazon.com/AmazonS3/latest/gsg/CreatingABucket.html).

Then, locate the following section in the `access.properties` file and fill in the name of an S3
bucket that you want the sample to use:

        ####### FileProcessing Sample Config Values ##########
        Workflow.Input.TargetBucketName=<Your S3 bucket name>

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=ActivityHost
    mvn exec:java -Dexec.mainClass=WorkflowHost
    mvn exec:java -Dexec.mainClass=WorkflowExecutionStarter

### Periodic Workflow

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=PeriodicWorkflowWorker
    mvn exec:java -Dexec.mainClass=com.amazonaws.services.simpleworkflow.flow.examples.periodicworkflow.WorkflowHost
    mvn exec:java -Dexec.mainClass=WorkflowExecutionStarter

### Split Merge

The sample has three executables. You should run each command in a separate terminal window, from
the `samples` directory.

    mvn exec:java -Dexec.mainClass=ActivityHost
    mvn exec:java -Dexec.mainClass=WorkflowHost
    mvn exec:java -Dexec.mainClass=WorkflowExecutionStarter

