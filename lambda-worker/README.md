# Lambda Worker

This sample demonstrates a Temporal Java Worker running inside an AWS Lambda function.
It registers a simple greeting Workflow and Activity, configures Worker Deployment
Versioning, and includes helper scripts for packaging the Lambda and configuring Temporal
Cloud invocation.

This sample uses the `1.37.0-SNAPSHOT` SDK artifacts for AWS Lambda worker support. For
local SDK co-development, create `settings.local.gradle` in the repository root and add
`includeBuild '../sdk-java'`. That file is ignored by Git and makes Gradle use the local
SDK checkout instead of resolving snapshot artifacts.

## Prerequisites

- Java 17+
- AWS CLI configured with permissions to create Lambda functions, IAM roles, and
  CloudFormation stacks
- A Temporal Cloud namespace with Serverless Workers enabled, or a self-hosted Temporal
  Service configured for AWS Lambda Serverless Workers
- A Temporal Cloud API key. This walkthrough deploys it as a Lambda environment variable
  because these are development-only secrets.

## Files

| File | Description |
|------|-------------|
| `src/main/java/io/temporal/samples/lambdaworker/LambdaFunction.java` | AWS Lambda handler |
| `src/main/java/io/temporal/samples/lambdaworker/LambdaWorkerSample.java` | Shared task queue, deployment version, and worker registrations |
| `src/main/java/io/temporal/samples/lambdaworker/SampleWorkflow*.java` | Sample Workflow interface and implementation |
| `src/main/java/io/temporal/samples/lambdaworker/GreetingActivities*.java` | Sample Activity interface and implementation |
| `src/main/java/io/temporal/samples/lambdaworker/Starter.java` | Local helper that starts a Workflow for the Lambda worker |
| `temporal.toml.sample` | Temporal client connection configuration |
| `otel-collector-config.yaml.sample` | Optional ADOT collector configuration |
| `deploy-lambda.sh` | Builds and uploads the Lambda deployment package |
| `mk-iam-role.sh` | Creates the role Temporal Cloud assumes to invoke Lambda |
| `iam-role-for-temporal-lambda-invoke-test.yaml` | CloudFormation template for the invocation role |
| `extra-setup-steps` | Optional IAM and Lambda settings for OpenTelemetry |

## Build

```bash
./gradlew :lambda-worker:test
./gradlew :lambda-worker:shadowJar
```

The Lambda handler string is:

```text
io.temporal.samples.lambdaworker.LambdaFunction::handleRequest
```

## Configure Environment

Set AWS, Temporal, and sample names first. Use unique values if you share the account or
namespace with other developers.

```bash
export AWS_PROFILE=<aws-profile>
export AWS_REGION=us-west-2
export AWS_DEFAULT_REGION="$AWS_REGION"

export TEMPORAL_ADDRESS=<your-namespace>.<account>.tmprl.cloud:7233
export TEMPORAL_NAMESPACE=<your-namespace>.<account>
export TEMPORAL_API_KEY=<your-api-key>
export TEMPORAL_TLS=true

export FUNCTION_NAME=my-temporal-java-worker
export EXECUTION_ROLE_NAME="${FUNCTION_NAME}-exec"
export STACK_NAME="${FUNCTION_NAME}-invoke"
export EXTERNAL_ID="${FUNCTION_NAME}-external-id"

export DEPLOYMENT_NAME=my-app
export BUILD_ID=build-1
export TASK_QUEUE=serverless-task-queue-java
export WORKFLOW_PREFIX=serverless-workflow-id-java
```

The Lambda worker reads these environment variables:

```bash
TEMPORAL_ADDRESS
TEMPORAL_NAMESPACE
TEMPORAL_API_KEY
TEMPORAL_TASK_QUEUE
TEMPORAL_WORKER_DEPLOYMENT_NAME
TEMPORAL_WORKER_BUILD_ID
```

The local starter also reads `TEMPORAL_TASK_QUEUE` and `TEMPORAL_WORKFLOW_ID_PREFIX`.

You can also use `temporal.toml.sample` as a starting point for `temporal.toml` and set
`TEMPORAL_CONFIG_FILE=temporal.toml`.

`TEMPORAL_TASK_QUEUE`, `TEMPORAL_WORKER_DEPLOYMENT_NAME`,
`TEMPORAL_WORKER_BUILD_ID`, and `TEMPORAL_WORKFLOW_ID_PREFIX` are optional. The values
above are the sample defaults.

## Deploy Lambda

Create the Lambda execution role:

```bash
cat > /tmp/temporal-lambda-trust-policy.json <<'JSON'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
JSON

aws iam create-role \
  --role-name "$EXECUTION_ROLE_NAME" \
  --assume-role-policy-document file:///tmp/temporal-lambda-trust-policy.json \
  --query 'Role.Arn' \
  --output text

aws iam attach-role-policy \
  --role-name "$EXECUTION_ROLE_NAME" \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

export EXECUTION_ROLE_ARN="$(
  aws iam get-role \
    --role-name "$EXECUTION_ROLE_NAME" \
    --query 'Role.Arn' \
    --output text
)"
```

Build the deployment jar and create the Java 17 Lambda function. The jar is small enough
for direct upload in this sample; use S3 if your local artifact grows beyond Lambda's
direct upload limit.

```bash
./gradlew :lambda-worker:shadowJar

aws lambda create-function \
  --function-name "$FUNCTION_NAME" \
  --runtime java17 \
  --handler io.temporal.samples.lambdaworker.LambdaFunction::handleRequest \
  --role "$EXECUTION_ROLE_ARN" \
  --zip-file fileb://lambda-worker/build/libs/lambda-worker-1.0.0-all.jar \
  --environment "Variables={TEMPORAL_ADDRESS=$TEMPORAL_ADDRESS,TEMPORAL_NAMESPACE=$TEMPORAL_NAMESPACE,TEMPORAL_API_KEY=$TEMPORAL_API_KEY,TEMPORAL_TASK_QUEUE=$TASK_QUEUE,TEMPORAL_WORKER_DEPLOYMENT_NAME=$DEPLOYMENT_NAME,TEMPORAL_WORKER_BUILD_ID=$BUILD_ID}" \
  --timeout 90 \
  --memory-size 1024 \
  --query 'FunctionArn' \
  --output text

aws lambda wait function-active --function-name "$FUNCTION_NAME"

export FUNCTION_ARN="$(
  aws lambda get-function \
    --function-name "$FUNCTION_NAME" \
    --query 'Configuration.FunctionArn' \
    --output text
)"
```

To update code after the function exists:

```bash
./lambda-worker/deploy-lambda.sh "$FUNCTION_NAME"
```

If direct upload is too large, set `LAMBDA_CODE_S3_BUCKET` and rerun:

```bash
LAMBDA_CODE_S3_BUCKET=<code-bucket> ./lambda-worker/deploy-lambda.sh "$FUNCTION_NAME"
```

## Configure Invocation

Create the IAM role that Temporal Cloud assumes to invoke the Lambda:

```bash
cd lambda-worker
./mk-iam-role.sh "$STACK_NAME" "$EXTERNAL_ID" "$FUNCTION_ARN"
cd ..

aws cloudformation wait stack-create-complete --stack-name "$STACK_NAME"

export INVOCATION_ROLE_ARN="$(
  aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='RoleARN'].OutputValue | [0]" \
    --output text
)"
```

Create and route the Worker Deployment Version:

```bash
temporal worker deployment create --name "$DEPLOYMENT_NAME"

temporal worker deployment create-version \
  --deployment-name "$DEPLOYMENT_NAME" \
  --build-id "$BUILD_ID" \
  --aws-lambda-function-arn "$FUNCTION_ARN" \
  --aws-lambda-assume-role-arn "$INVOCATION_ROLE_ARN" \
  --aws-lambda-assume-role-external-id "$EXTERNAL_ID"

temporal worker deployment set-current-version \
  --deployment-name "$DEPLOYMENT_NAME" \
  --build-id "$BUILD_ID" \
  --allow-no-pollers \
  --yes
```

An async Lambda smoke test returns immediately and should produce worker startup logs:

```bash
aws lambda invoke \
  --function-name "$FUNCTION_NAME" \
  --invocation-type Event \
  --cli-binary-format raw-in-base64-out \
  --payload '{}' \
  /tmp/lambda-worker-response.json \
  --query 'StatusCode' \
  --output text
```

A synchronous invoke can run until the Lambda worker exits near the function timeout. If
you want to wait for that path, set the AWS CLI read timeout higher than the function
timeout.

## Start Workflow

After the Worker Deployment Version is current, start the sample Workflow:

```bash
export TEMPORAL_TASK_QUEUE="$TASK_QUEUE"
export TEMPORAL_WORKFLOW_ID_PREFIX="$WORKFLOW_PREFIX"

./gradlew -q :lambda-worker:execute \
  -PmainClass=io.temporal.samples.lambdaworker.Starter
```

The starter only creates a Workflow Execution. It does not start a local Worker. The
important value is `TEMPORAL_TASK_QUEUE`; it must match the task queue configured on the
Lambda function.

## Local SDK Development

For local development of the Workflow and Activity logic, run the unit tests. They use
`TestWorkflowRule` and do not require AWS or a running Temporal Service.
