#!/bin/bash

# Variables
CODE_BUCKET=$(cat bucket-name-for-code.txt)
INPUT_BUCKET=$(cat bucket-name-for-videos-input.txt)
OUTPUT_BUCKET=$(cat bucket-name-for-videos-output.txt)
ROLE_NAME="ElasticTranscoder_Role"
POLICY_NAME="ElasticTranscoderPolicy"
PIPELINE_NAME="VideoPipeline"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Function to check if IAM role exists
role_exists() {
  aws iam get-role --role-name $1 &> /dev/null
}

# Create IAM role trust policy document
cat > transcoder-trust-policy.json << EOL
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "elastictranscoder.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOL

# Check if the IAM role exists before creating it
if role_exists $ROLE_NAME; then
  echo "Role $ROLE_NAME already exists. Skipping role creation."
else
  # Create IAM role
  aws iam create-role --role-name $ROLE_NAME --assume-role-policy-document file://transcoder-trust-policy.json
  echo "Role $ROLE_NAME created successfully."
fi

# Create IAM policy document
cat > transcoder-policy.json << EOL
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": [
        "arn:aws:s3:::$INPUT_BUCKET",
        "arn:aws:s3:::$OUTPUT_BUCKET"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:GetObjectAcl",
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::$INPUT_BUCKET/*",
        "arn:aws:s3:::$OUTPUT_BUCKET/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": "elastictranscoder:*",
      "Resource": "*"
    }
  ]
}
EOL

# Attach policy to role
aws iam put-role-policy --role-name $ROLE_NAME --policy-name $POLICY_NAME --policy-document file://transcoder-policy.json

# Create Elastic Transcoder pipeline
PIPELINE_ARN="arn:aws:iam::$ACCOUNT_ID:role/$ROLE_NAME"
PIPELINE_JSON=$(aws elastictranscoder create-pipeline \
  --name $PIPELINE_NAME \
  --input-bucket $INPUT_BUCKET \
  --role $PIPELINE_ARN \
  --content-config "{\"Bucket\":\"$OUTPUT_BUCKET\",\"Permissions\":[{\"GranteeType\":\"Group\",\"Grantee\":\"AllUsers\",\"Access\":[\"FullControl\"]}]}" \
  --thumbnail-config "{\"Bucket\":\"$OUTPUT_BUCKET\",\"Permissions\":[{\"GranteeType\":\"Group\",\"Grantee\":\"AllUsers\",\"Access\":[\"FullControl\"]}]}")

# Extract pipeline ID
PIPELINE_ID=$(echo $PIPELINE_JSON | jq -r '.Pipeline.Id')

# Store pipeline ID in a .txt file
echo $PIPELINE_ID > pipeline-id.txt

# Upload the file to S3
aws s3 cp pipeline-id.txt s3://$CODE_BUCKET/pipeline-id.txt

# Clean up temporary files
rm transcoder-trust-policy.json transcoder-policy.json

echo "Elastic Transcoder pipeline created successfully. Pipeline ID: $PIPELINE_ID"
