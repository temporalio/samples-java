#!/bin/bash
set -euo pipefail

STACK_NAME="${1:?Usage: mk-iam-role.sh <stack-name> <external-id> <lambda-arn>}"
EXTERNAL_ID="${2:?Usage: mk-iam-role.sh <stack-name> <external-id> <lambda-arn>}"
LAMBDA_ARN="${3:?Usage: mk-iam-role.sh <stack-name> <external-id> <lambda-arn>}"

aws cloudformation create-stack \
  --stack-name "$STACK_NAME" \
  --template-body file://iam-role-for-temporal-lambda-invoke-test.yaml \
  --parameters \
    ParameterKey=AssumeRoleExternalId,ParameterValue="$EXTERNAL_ID" \
    ParameterKey=LambdaFunctionARNs,ParameterValue="\"$LAMBDA_ARN\"" \
  --capabilities CAPABILITY_NAMED_IAM
