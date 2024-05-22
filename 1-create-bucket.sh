#!/bin/bash

# Function to generate a random bucket ID
generate_bucket_id() {
  echo $(dd if=/dev/random bs=8 count=1 2>/dev/null | od -An -tx1 | tr -d ' \t\n')
}

# Generate bucket IDs
BUCKET_ID_CODE=$(generate_bucket_id)
BUCKET_ID_VIDEOS_INPUT=$(generate_bucket_id)
BUCKET_ID_VIDEOS_OUTPUT=$(generate_bucket_id)

# Construct bucket names
BUCKET_NAME_CODE=lambda-artifacts-code-$BUCKET_ID_CODE
BUCKET_NAME_VIDEOS_INPUT=lambda-artifacts-videos-$BUCKET_ID_VIDEOS_INPUT
BUCKET_NAME_VIDEOS_OUTPUT=lambda-artifacts-videos-$BUCKET_ID_VIDEOS_OUTPUT

# Print bucket names to files
echo $BUCKET_NAME_CODE > bucket-name-for-code.txt
echo $BUCKET_NAME_VIDEOS_INPUT > bucket-name-for-videos-input.txt
echo $BUCKET_NAME_VIDEOS_OUTPUT > bucket-name-for-videos-output.txt

# Create the S3 buckets
aws s3 mb s3://$BUCKET_NAME_CODE
aws s3 mb s3://$BUCKET_NAME_VIDEOS_INPUT
aws s3 mb s3://$BUCKET_NAME_VIDEOS_OUTPUT

echo "Created S3 buckets:"
echo "Code Bucket: $BUCKET_NAME_CODE"
echo "Video Input Bucket: $BUCKET_NAME_VIDEOS_INPUT"
echo "Video Output Bucket: $BUCKET_NAME_VIDEOS_OUTPUT"