#!/bin/bash
set -euo pipefail

STACK_NAME="${1:?Usage: mk-iam-role.sh <stack-name> <external-id> <lambda-arn-pattern>}"
EXTERNAL_ID="${2:?Usage: mk-iam-role.sh <stack-name> <external-id> <lambda-arn-pattern>}"
LAMBDA_ARN_PATTERN="${3:?Usage: mk-iam-role.sh <stack-name> <external-id> <lambda-arn-pattern>}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

aws cloudformation create-stack \
  --stack-name "$STACK_NAME" \
  --template-body "file://${SCRIPT_DIR}/temporal-cloud-lambda-invoke-role.yaml" \
  --parameters \
    ParameterKey=AssumeRoleExternalId,ParameterValue="$EXTERNAL_ID" \
    ParameterKey=LambdaFunctionARNs,ParameterValue="\"$LAMBDA_ARN_PATTERN\"" \
  --capabilities CAPABILITY_IAM
