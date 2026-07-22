#!/bin/bash
set -euo pipefail

ROLE_NAME="${1:?Usage: enable-telemetry.sh <role-name> <function-name> <region> <account-id>}"
FUNCTION_NAME="${2:?Usage: enable-telemetry.sh <role-name> <function-name> <region> <account-id>}"
REGION="${3:?Usage: enable-telemetry.sh <role-name> <function-name> <region> <account-id>}"
ACCOUNT_ID="${4:?Usage: enable-telemetry.sh <role-name> <function-name> <region> <account-id>}"
ADOT_COLLECTOR_LAYER_ARN="${ADOT_COLLECTOR_LAYER_ARN:-arn:aws:lambda:${REGION}:901920570463:layer:aws-otel-collector-amd64-ver-0-117-0:1}"

aws iam put-role-policy \
  --role-name "$ROLE_NAME" \
  --policy-name ADOT-Telemetry-Permissions \
  --policy-document "{
    \"Version\": \"2012-10-17\",
    \"Statement\": [
      {
        \"Effect\": \"Allow\",
        \"Action\": [
          \"logs:CreateLogGroup\",
          \"logs:CreateLogStream\",
          \"logs:PutLogEvents\"
        ],
        \"Resource\": \"arn:aws:logs:${REGION}:${ACCOUNT_ID}:log-group:/aws/lambda/${FUNCTION_NAME}:*\"
      },
      {
        \"Effect\": \"Allow\",
        \"Action\": [
          \"xray:PutTraceSegments\",
          \"xray:PutTelemetryRecords\"
        ],
        \"Resource\": \"*\"
      }
    ]
  }"

aws lambda update-function-configuration \
  --function-name "$FUNCTION_NAME" \
  --layers "$ADOT_COLLECTOR_LAYER_ARN" \
  --tracing-config Mode=Active \
  --query 'FunctionArn' \
  --output text
