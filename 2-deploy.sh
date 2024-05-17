#!/bin/bash
set -eo pipefail

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

# Load bucket names from files
if [ -f "bucket-name-for-code.txt" ]; then
  ARTIFACT_CODE_BUCKET=$(cat bucket-name-for-code.txt)
  log "Using artifact code bucket: $ARTIFACT_CODE_BUCKET"
else
  log "Error: bucket-name-for-code.txt not found."
  exit 1
fi

if [ -f "bucket-name-for-videos.txt" ]; then
  ARTIFACT_VIDEO_BUCKET=$(cat bucket-name-for-videos.txt)
  log "Using artifact video bucket: $ARTIFACT_VIDEO_BUCKET"
else
  log "Error: bucket-name-for-videos.txt not found."
  exit 1
fi

# Select the appropriate template based on the parameter
TEMPLATE=template.yaml
if [ $1 ]
then
  if [ $1 = mvn ]
  then
    TEMPLATE=template.yaml
    mvn package
  fi
else
  gradle build -i
fi

log "Packaging with template: $TEMPLATE"

# Package and deploy
aws cloudformation package --template-file $TEMPLATE --s3-bucket $ARTIFACT_CODE_BUCKET --output-template-file out.yml
aws cloudformation deploy --template-file out.yml --stack-name youtube-demo --parameter-overrides ExistingBucketName=$ARTIFACT_VIDEO_BUCKET --capabilities CAPABILITY_NAMED_IAM

log "Deployment complete.: $ARTIFACT_VIDEO_BUCKET"