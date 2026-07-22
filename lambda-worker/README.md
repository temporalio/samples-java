# Lambda Worker

This sample demonstrates a Temporal Java Worker running inside an AWS Lambda function.
It registers a simple greeting Workflow and Activity, configures Worker Deployment
Versioning, and includes helper scripts for packaging the Lambda and configuring Temporal
Cloud invocation.

The deployable Worker and local Workflow starter are separate Gradle projects, so
starter-only code and dependencies are not included in the Lambda artifact.

It uses the same published Temporal Java SDK version as the other samples in this repository.

## Prerequisites

- Java 17+
- AWS CLI configured with permissions to create Lambda functions, IAM roles, and
  CloudFormation stacks
- An AWS-hosted Temporal Cloud namespace with Serverless Workers enabled, or a
  [self-hosted Temporal Service](https://docs.temporal.io/production-deployment/worker-deployments/serverless-workers/self-hosted-setup)
  version 1.31.0 or later with the AWS Lambda Worker Controller setup completed
- A Temporal Cloud API key (if using Temporal Cloud). This walkthrough deploys it as a
  Lambda environment variable because these are development-only secrets.

## Layout

- `worker/` contains the Lambda handler, Workflow, Activity, and deployable Worker project.
- `starter/` contains the local Workflow starter project.
- `deploy/` contains the AWS deployment scripts and CloudFormation template.
- `temporal.template.toml` is a Temporal connection configuration template.
- `otel-collector-config.template.yaml` configures the ADOT collector packaged with the
  Lambda Worker.

## Build

```bash
./gradlew :lambda-worker:worker:test
./gradlew :lambda-worker:worker:shadowJar
```

The `shadowJar` task packages `otel-collector-config.template.yaml` at the root of the Lambda
artifact as `otel-collector-config.yaml`.

The Lambda handler string is:

```text
io.temporal.samples.lambdaworker.LambdaFunction::handleRequest
```

## Configure Environment

Set AWS, Temporal, and sample names first. Use unique values if you share the account or
namespace with other developers. The connection values below are for Temporal Cloud. For a
self-hosted Service, use its frontend address, Namespace, and TLS or authentication settings;
leave `TEMPORAL_API_KEY` unset if the Service does not require one.

```bash
export AWS_PROFILE=<aws-profile>
export AWS_REGION=us-west-2
export AWS_DEFAULT_REGION="$AWS_REGION"
export AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"

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
TEMPORAL_LAMBDA_DEPLOYMENT_NAME
TEMPORAL_LAMBDA_BUILD_ID
```

The ADOT collector extension reads
`OPENTELEMETRY_COLLECTOR_CONFIG_URI=/var/task/otel-collector-config.yaml`. The Java Lambda Worker
uses `OtelLambdaWorkerConfigurationHelper` to send Temporal traces and metrics to the collector
over OTLP. The collector exports traces to AWS X-Ray and metrics to the
`TemporalWorkerMetrics` CloudWatch namespace.

The local starter also reads `TEMPORAL_TASK_QUEUE` and
`TEMPORAL_LAMBDA_WORKFLOW_ID_PREFIX`.

You can also copy `lambda-worker/temporal.template.toml` to
`lambda-worker/temporal.toml`, fill in the connection details, and set
`TEMPORAL_CONFIG_FILE=lambda-worker/temporal.toml`. The `temporal.toml` file is ignored by Git.

`TEMPORAL_TASK_QUEUE`, `TEMPORAL_LAMBDA_DEPLOYMENT_NAME`,
`TEMPORAL_LAMBDA_BUILD_ID`, and `TEMPORAL_LAMBDA_WORKFLOW_ID_PREFIX` are optional. The
values above are the sample defaults.

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
./gradlew :lambda-worker:worker:shadowJar

aws lambda create-function \
  --function-name "$FUNCTION_NAME" \
  --runtime java17 \
  --handler io.temporal.samples.lambdaworker.LambdaFunction::handleRequest \
  --role "$EXECUTION_ROLE_ARN" \
  --zip-file fileb://lambda-worker/worker/build/libs/lambda-worker-1.0.0-all.jar \
  --environment "Variables={TEMPORAL_ADDRESS=$TEMPORAL_ADDRESS,TEMPORAL_NAMESPACE=$TEMPORAL_NAMESPACE,TEMPORAL_API_KEY=$TEMPORAL_API_KEY,TEMPORAL_TASK_QUEUE=$TASK_QUEUE,TEMPORAL_LAMBDA_DEPLOYMENT_NAME=$DEPLOYMENT_NAME,TEMPORAL_LAMBDA_BUILD_ID=$BUILD_ID,OPENTELEMETRY_COLLECTOR_CONFIG_URI=/var/task/otel-collector-config.yaml}" \
  --timeout 90 \
  --memory-size 1024 \
  --query 'FunctionArn' \
  --output text

aws lambda wait function-active --function-name "$FUNCTION_NAME"

./lambda-worker/deploy/enable-telemetry.sh \
  "$EXECUTION_ROLE_NAME" \
  "$FUNCTION_NAME" \
  "$AWS_REGION" \
  "$AWS_ACCOUNT_ID"

aws lambda wait function-updated --function-name "$FUNCTION_NAME"

export FUNCTION_BASE_ARN="$(
  aws lambda get-function \
    --function-name "$FUNCTION_NAME" \
    --query 'Configuration.FunctionArn' \
    --output text
)"

export FUNCTION_VERSION_ARN="$(
  aws lambda publish-version \
    --function-name "$FUNCTION_NAME" \
    --description "Build ID $BUILD_ID" \
    --query 'FunctionArn' \
    --output text
)"
```

`enable-telemetry.sh` grants the execution role permission to send traces and EMF logs, enables
active tracing, and attaches AWS's collector-only ADOT Lambda layer. Its default layer ARN is for
the sample's `x86_64` architecture in standard AWS regions. Set `ADOT_COLLECTOR_LAYER_ARN` before
running the script to use another compatible regional layer.

## Configure Invocation

For Temporal Cloud, create the IAM role that Temporal Cloud assumes to invoke the Lambda. The
wildcard suffix authorizes invocation of every immutable version published for this function:

```bash
./lambda-worker/deploy/mk-iam-role.sh \
  "$STACK_NAME" \
  "$EXTERNAL_ID" \
  "${FUNCTION_BASE_ARN}:*"

aws cloudformation wait stack-create-complete --stack-name "$STACK_NAME"

export INVOCATION_ROLE_ARN="$(
  aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='RoleARN'].OutputValue | [0]" \
    --output text
)"
```

The included CloudFormation template trusts Temporal Cloud's AWS identities and must not be
used for a self-hosted Temporal Service. For self-hosted deployments, complete the
[self-hosted Serverless Workers setup](https://docs.temporal.io/production-deployment/worker-deployments/serverless-workers/self-hosted-setup),
then set `INVOCATION_ROLE_ARN` to the role created by that process.

## Create Worker Deployment Version

Create and route the Worker Deployment Version. The Temporal CLI can connect to either Temporal
Cloud or a self-hosted Service using the connection configuration above.

```bash
temporal worker deployment create --name "$DEPLOYMENT_NAME"

temporal worker deployment create-version \
  --deployment-name "$DEPLOYMENT_NAME" \
  --build-id "$BUILD_ID" \
  --aws-lambda-function-arn "$FUNCTION_VERSION_ARN" \
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
  --function-name "$FUNCTION_VERSION_ARN" \
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

## Deploy an Updated Version

Each Temporal Build ID should point to an immutable Lambda function version. To deploy an
update, choose a new Build ID, update the Lambda environment, upload the new code, and publish a
new Lambda version:

```bash
export BUILD_ID=build-2

aws lambda update-function-configuration \
  --function-name "$FUNCTION_NAME" \
  --environment "Variables={TEMPORAL_ADDRESS=$TEMPORAL_ADDRESS,TEMPORAL_NAMESPACE=$TEMPORAL_NAMESPACE,TEMPORAL_API_KEY=$TEMPORAL_API_KEY,TEMPORAL_TASK_QUEUE=$TASK_QUEUE,TEMPORAL_LAMBDA_DEPLOYMENT_NAME=$DEPLOYMENT_NAME,TEMPORAL_LAMBDA_BUILD_ID=$BUILD_ID,OPENTELEMETRY_COLLECTOR_CONFIG_URI=/var/task/otel-collector-config.yaml}" \
  --query 'FunctionArn' \
  --output text

aws lambda wait function-updated --function-name "$FUNCTION_NAME"

./lambda-worker/deploy/deploy-lambda.sh "$FUNCTION_NAME"

aws lambda wait function-updated --function-name "$FUNCTION_NAME"

export FUNCTION_VERSION_ARN="$(
  aws lambda publish-version \
    --function-name "$FUNCTION_NAME" \
    --description "Build ID $BUILD_ID" \
    --query 'FunctionArn' \
    --output text
)"
```

If direct upload is too large, set `LAMBDA_CODE_S3_BUCKET` when running `deploy-lambda.sh`:

```bash
LAMBDA_CODE_S3_BUCKET=<code-bucket> ./lambda-worker/deploy/deploy-lambda.sh "$FUNCTION_NAME"
```

Create the new Worker Deployment Version and make it current using the commands in
[Create Worker Deployment Version](#create-worker-deployment-version), omitting the
`temporal worker deployment create` command because the deployment already exists. Existing
Worker Deployment Versions continue to reference their original Lambda versions.

## Start Workflow

After the Worker Deployment Version is current, start the sample Workflow:

```bash
export TEMPORAL_TASK_QUEUE="$TASK_QUEUE"
export TEMPORAL_LAMBDA_WORKFLOW_ID_PREFIX="$WORKFLOW_PREFIX"

./gradlew -q :lambda-worker:starter:execute
```

The starter only creates a Workflow Execution. It does not start a local Worker. The
important value is `TEMPORAL_TASK_QUEUE`; it must match the task queue configured on the
Lambda function.

## Verify OpenTelemetry

After invoking the Lambda or completing a Workflow, inspect the function logs for ADOT collector
startup and export messages:

```bash
aws logs tail "/aws/lambda/$FUNCTION_NAME" --since 10m
```

Temporal SDK metrics should also appear in the `TemporalWorkerMetrics` CloudWatch namespace:

```bash
aws cloudwatch list-metrics \
  --namespace TemporalWorkerMetrics \
  --query 'Metrics[].MetricName' \
  --output text
```

Temporal tracing spans are exported to AWS X-Ray and can be inspected in the X-Ray trace view.

## Local SDK Development

For local development of the Workflow and Activity logic, run the unit tests. They use
`TestWorkflowRule` and do not require AWS or a running Temporal Service.

```bash
./gradlew :lambda-worker:worker:test
```
