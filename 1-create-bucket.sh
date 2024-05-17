#!/bin/bash

# Function to generate a random bucket ID
generate_bucket_id() {
  echo $(dd if=/dev/random bs=8 count=1 2>/dev/null | od -An -tx1 | tr -d ' \t\n')
}

# Generate bucket IDs
BUCKET_ID_CODE=$(generate_bucket_id)
BUCKET_ID_VIDEOS=$(generate_bucket_id)

# Construct bucket names
BUCKET_NAME_CODE=lambda-artifacts-code-$BUCKET_ID_CODE
BUCKET_NAME_VIDEOS=lambda-artifacts-videos-$BUCKET_ID_VIDEOS

# Print bucket names to files
echo $BUCKET_NAME_CODE > bucket-name-for-code.txt
echo $BUCKET_NAME_VIDEOS > bucket-name-for-videos.txt

# Create the S3 buckets
aws s3 mb s3://$BUCKET_NAME_CODE
aws s3 mb s3://$BUCKET_NAME_VIDEOS

echo "Created S3 buckets:"
echo "Code bucket: $BUCKET_NAME_CODE"
echo "Video bucket: $BUCKET_NAME_VIDEOS"