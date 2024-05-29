#!/bin/bash

REGION=$(aws configure get region)

# Create DynamoDB table: VideoTable
aws dynamodb create-table \
    --table-name "VideoTable" \
    --attribute-definitions \
        AttributeName=video_id,AttributeType=S \
    --key-schema \
        AttributeName=video_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION

echo "DynamoDB table VideoTable created successfully in region $REGION."

# Create DynamoDB table: UserTable
aws dynamodb create-table \
    --table-name "UserTable" \
    --attribute-definitions \
        AttributeName=user_id,AttributeType=S \
    --key-schema \
        AttributeName=user_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION

echo "DynamoDB table UserTable created successfully in region $REGION."

# Create DynamoDB table: CommentsTable
aws dynamodb create-table \
    --table-name "CommentsTable" \
    --attribute-definitions \
        AttributeName=coments_id,AttributeType=S \
    --key-schema \
        AttributeName=comments_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION

echo "DynamoDB table CommentsTable created successfully in region $REGION."

# Create DynamoDB table: ChannelTable
aws dynamodb create-table \
    --table-name "ChannelTable" \
    --attribute-definitions \
        AttributeName=channel_id,AttributeType=S \
    --key-schema \
        AttributeName=channel_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION

echo "DynamoDB table ChannelTable created successfully in region $REGION."

# Create DynamoDB table: LikeOrDislikeTable
aws dynamodb create-table \
    --table-name "LikeOrDislikeTable" \
    --attribute-definitions \
        AttributeName=video_id,AttributeType=S \
        AttributeName=user_id,AttributeType=S \
    --key-schema \
        AttributeName=video_id,KeyType=HASH \
        AttributeName=user_id,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION


echo "DynamoDB table LikeOrDislikeTable created successfully in region $REGION."