#!/bin/bash
set -euo pipefail

FUNCTION_NAME="${1:?Usage: deploy-lambda.sh <function-name>}"
MAX_DIRECT_UPLOAD_BYTES=50000000

REPOSITORY_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPOSITORY_DIR"
./gradlew :lambda-worker:worker:shadowJar

JAR_FILE="$(find lambda-worker/worker/build/libs -name 'lambda-worker-*-all.jar' | head -n 1)"

if stat -f%z "$JAR_FILE" >/dev/null 2>&1; then
  JAR_SIZE="$(stat -f%z "$JAR_FILE")"
else
  JAR_SIZE="$(stat -c%s "$JAR_FILE")"
fi

if [[ -n "${LAMBDA_CODE_S3_BUCKET:-}" ]]; then
  S3_KEY="${LAMBDA_CODE_S3_KEY:-lambda-worker/$(basename "$JAR_FILE")}"
  aws s3 cp "$JAR_FILE" "s3://$LAMBDA_CODE_S3_BUCKET/$S3_KEY"
  aws lambda update-function-code \
    --function-name "$FUNCTION_NAME" \
    --s3-bucket "$LAMBDA_CODE_S3_BUCKET" \
    --s3-key "$S3_KEY"
  exit 0
fi

if (( JAR_SIZE > MAX_DIRECT_UPLOAD_BYTES )); then
  echo "Artifact is ${JAR_SIZE} bytes, which is too large for direct Lambda upload." >&2
  echo "Set LAMBDA_CODE_S3_BUCKET and rerun to upload through S3." >&2
  exit 1
fi

aws lambda update-function-code --function-name "$FUNCTION_NAME" --zip-file "fileb://$JAR_FILE"
