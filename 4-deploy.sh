#!/bin/bash
set -eo pipefail

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region)
TranscoderFunction="TranscoderFunction"  # Your specific keyword

# Extract the Lambda function name
TRANSCODER_FUNCTION_NAME=$(aws lambda list-functions | jq -r --arg keyword "$TranscoderFunction" '.Functions[] | select(.FunctionName | contains($keyword)) | .FunctionName')

# Verify the extracted function name
echo "Extracted Lambda Function Name: $TRANSCODER_FUNCTION_NAME"

# Define function for logging
log() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $*"
}
log "Starting the deployment script."

# Check for required commands
if ! command -v aws &>/dev/null && ! command -v mvn &>/dev/null; then
    log "Error: Necessary tools are not installed."
    exit 1
fi

# Load bucket names and transcoder pipeline id from files
if [ -f "bucket-name-for-code.txt" ]; then
  ARTIFACT_CODE_BUCKET=$(cat bucket-name-for-code.txt)
  log "Using artifact code bucket: $ARTIFACT_CODE_BUCKET"
else
  log "Error: bucket-name-for-code.txt not found."
  exit 1
fi

if [ -f "bucket-name-for-videos-input.txt" ]; then
  ARTIFACT_VIDEO_INPUT_BUCKET=$(cat bucket-name-for-videos-input.txt)
  log "Using artifact video input bucket: $ARTIFACT_VIDEO_INPUT_BUCKET"
else
  log "Error: bucket-name-for-videos-input.txt not found."
  exit 1
fi

if [ -f "bucket-name-for-videos-output.txt" ]; then
  ARTIFACT_VIDEO_OUTPUT_BUCKET=$(cat bucket-name-for-videos-output.txt)
  log "Using artifact video output bucket: $ARTIFACT_VIDEO_ARTIFACT_VIDEO_OUTPUT_BUCKET"
else
  log "Error: bucket-name-for-videos-output.txt not found."
  exit 1
fi

if [ -f "pipeline-id.txt" ]; then
  PIPELINE_ID=$(cat pipeline-id.txt)
  log "Using transcoder pipeline-id: $PIPELINE_ID"
else
  log "Error: pipeline-id.txt not found."
  exit 1
fi

# Select the appropriate template based on the parameter
TEMPLATE=template.yaml
if [ $1 ]; then
  if [ $1 = mvn ]; then
    TEMPLATE=template.yaml
    mvn package
  fi
else
  gradle build -i
fi

log "Packaging with template: $TEMPLATE"

# Package and deploy for video
aws cloudformation package --template-file $TEMPLATE --s3-bucket $ARTIFACT_CODE_BUCKET --output-template-file out.yml
aws cloudformation deploy \
    --template-file out.yml \
    --stack-name youtube-demo \
    --parameter-overrides ExistingInputBucketName=$ARTIFACT_VIDEO_INPUT_BUCKET ExistingOutputBucketName=$ARTIFACT_VIDEO_OUTPUT_BUCKET ExistingTranscoderPipelineId=$PIPELINE_ID\
    --capabilities CAPABILITY_NAMED_IAM

log "Deployment complete."
log "Artifact code bucket: $ARTIFACT_CODE_BUCKET"
log "Artifact video input bucket: $ARTIFACT_VIDEO_INPUT_BUCKET"
log "Artifact video output bucket: $ARTIFACT_VIDEO_OUTPUT_BUCKET"
log "Transcoder pipeline ID: $PIPELINE_ID"