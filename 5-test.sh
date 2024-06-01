#!/bin/bash
set -eo pipefail

# likeOrDislikeTable exist
# VideoTable exist

# VideoMetadata request JSON payload
VideoMetadata_item='{
    "video_id": {"S": "1234567"},
    "channel_id": {"S": "technology"},
    "creator_id": {"S": "HM"},
    "default_language": {"S": "English"},
    "description": {"S": "learning system Design"},
    "dislikes_count": {"N": "0"},
    "likes_count": {"N": "0"},
    "title": {"S": "test for hm"},
    "upload_date": {"S": "2024-05-30T00:54:46.396010087"},
    "video_URL": {"S": "url"},
    "views_count": {"N": "0"}
}'

#insert an Item in DynamoDB
aws dynamodb put-item \
    --table-name VideoTable \
    --item "$VideoMetadata_item"

API_JSON_PAYLOAD='{
    "video_id": "1234567",
    "user_id": "33"
}'

LIKE_API_URL="https://scrtswk623.execute-api.us-west-1.amazonaws.com/dev/v1/database/like"
DISLIKE_API_URL="https://rxam7k7k29.execute-api.us-west-1.amazonaws.com/dev/v1/database/dislike"
REMOVE_LIKE_API_URL="https://55x371uu9g.execute-api.us-west-1.amazonaws.com/dev/v1/database/remove-like"
REMOVE_DISLIKE_API_URL="https://pgkz4pnb7j.execute-api.us-west-1.amazonaws.com/dev/v1/database/remove-dislike"

expected_response_status_200="200";
expected_response_status_404="404";
echo "1. first Like: ==========================================================================================="
Api_response1=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $LIKE_API_URL)
expected_response_body1='{
  "likeDislikeTableItem": {
    "like": 1,
    "user_id": "33",
    "video_id": "1234567"
  },
  "videoTableItem": {
    "likes_count" : 1,
    "dislikes_count" : 0,
    "creator_id" : "HM",
    "description" : "learning system Design",
    "views_count" : 0,
    "default_language" : "English",
    "video_URL" : "url",
    "title" : "test for hm",
    "channel_id" : "technology",
    "video_id" : "1234567"
  }
}'

# Separate the response body and status code
http_status1=$(echo "$Api_response1" | tail -n1)
http_body1=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status1" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status1"
else
    echo "Status code mismatch: received $http_status1, expected $expected_response_status1"
fi

# Compare the response body using jq
if echo "$http_body1" | jq --argjson expected "$expected_response_body1" '
    (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
    (.videoTableItem | del(.upload_date) == $expected.videoTableItem)' ; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body1, expected $expected_response_body1"
fi

echo "# 2. Like second time====================================================================================="
Api_response2=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $LIKE_API_URL)
expected_response_body2='{
  "message": "Repeated requests."
}'
# Separate the response body and status code
http_status2=$(echo "$Api_response2" | tail -n1)
http_body2=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status2" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status"
else
    echo "Status code mismatch: received $http_status2, expected $expected_response_status_200"
fi

# Compare the response body using jq
if [[ "$http_body2" == $expected_response_body2 ]]; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body2, expected $expected_response_body2"
fi

echo "# 3. change to dislike==================================================================================="
Api_response3=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $DISLIKE_API_URL)

expected_response_body3='{
  "likeDislikeTableItem": {
    "like": -1,
    "user_id": "33",
    "video_id": "1234567"
  },
  "videoTableItem": {
    "likes_count" : 0,
    "dislikes_count" : 1,
    "creator_id" : "HM",
    "description" : "learning system Design",
    "views_count" : 0,
    "default_language" : "English",
    "video_URL" : "url",
    "title" : "test for hm",
    "channel_id" : "technology",
    "video_id" : "1234567"
  }
}'

# Separate the response body and status code
http_status3=$(echo "$Api_response3" | tail -n1)
http_body3=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status3" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status3"
else
    echo "Status code mismatch: received $http_status3, expected $expected_response_status_200"
fi

# Compare the response body using jq
if echo "$http_body3" | jq --argjson expected "$expected_response_body3" '
       (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
       (.videoTableItem | del(.upload_date) == $expected.videoTableItem)'; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body3, expected $expected_response_body3"
fi

echo "# 4. change to like ====================================================================================="
Api_response4=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $LIKE_API_URL)

expected_response_body4='{
  "likeDislikeTableItem": {
    "like": 1,
    "user_id": "33",
    "video_id": "1234567"
  },
  "videoTableItem": {
    "likes_count" : 1,
    "dislikes_count" : 0,
    "creator_id" : "HM",
    "description" : "learning system Design",
    "views_count" : 0,
    "default_language" : "English",
    "video_URL" : "url",
    "title" : "test for hm",
    "channel_id" : "technology",
    "video_id" : "1234567"
  }
}'

# Separate the response body and status code
http_status4=$(echo "$Api_response4" | tail -n1)
http_body4=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status4" == $expected_response_status4 ]]; then
    echo "Status code matches expected: $http_status4"
else
    echo "Status code mismatch: received $http_status4, expected $expected_response_status4"
fi

# Compare the response body using jq
if echo "$http_body4" | jq --argjson expected "$expected_response_body4" '
       (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
       (.videoTableItem | del(.upload_date) == $expected.videoTableItem)'; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body4, expected $expected_response_body4"
fi

echo "# 5. remove Like========================================================================================="
Api_response5=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $REMOVE_LIKE_API_URL)

#expected response
expected_response_body5='{
  "likes_count" : 0,
  "dislikes_count" : 0,
  "creator_id" : "HM",
  "description" : "learning system Design",
  "views_count" : 0,
  "default_language" : "English",
  "video_URL" : "url",
  "title" : "test for hm",
  "channel_id" : "technology",
  "video_id" : "1234567"
}'

# Separate the response body and status code
http_status5=$(echo "$Api_response5" | tail -n1)
http_body5=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status5" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status5"
else
    echo "Status code mismatch: received $http_status5, expected $expected_response_status_200"
fi

# Compare the response body using jq
if echo "$http_body5" | jq --argjson expected "$expected_response_body5" '
    (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
    (.videoTableItem | del(.upload_date) == $expected.videoTableItem)' ; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body5, expected $expected_response_body5"
fi

echo "# 6. Remove Like Again================================================================================"
Api_response6=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $REMOVE_LIKE_API_URL)

expected_response_body6='{
   "message": "likeOrDislikeTableItem not found."
}'

# Separate the response body and status code
http_status6=$(echo "$Api_response6" | tail -n1)
http_body6=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status6" == $expected_response_status_404 ]]; then
    echo "Status code matches expected: $http_status6"
else
    echo "Status code mismatch: received $http_status6, expected $expected_response_status_404"
fi

# Compare the response body using jq
if echo "$http_body6" | jq --argjson expected "$expected_response_body6" '
    (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
    (.videoTableItem | del(.upload_date) == $expected.videoTableItem)' ; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body6, expected $expected_response_body6"
fi

echo "#7. Dislike First Time================================================================================"
Api_response7=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $DISLIKE_API_URL)
expected_response_body7='{
  "likeDislikeTableItem": {
    "like": -1,
    "user_id": "33",
    "video_id": "1234567"
  },
  "videoTableItem": {
    "likes_count" : 0,
    "dislikes_count" : 1,
    "creator_id" : "HM",
    "description" : "learning system Design",
    "views_count" : 0,
    "default_language" : "English",
    "video_URL" : "url",
    "title" : "test for hm",
    "channel_id" : "technology",
    "video_id" : "1234567"
  }
}'
# Separate the response body and status code
http_status7=$(echo "$Api_response7" | tail -n1)
http_body7=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status7" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status7"
else
    echo "Status code mismatch: received $http_status7, expected $expected_response_status_200"
fi

# Compare the response body using jq
if echo "$http_body7" | jq --argjson expected "$expected_response_body7" '
       (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
       (.videoTableItem | del(.upload_date) == $expected.videoTableItem)'; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body7, expected $expected_response_body7"
fi

echo "#8. Dislike Second time================================================================================"
Api_response8=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $DISLIKE_API_URL)
expected_response_body8='{
  "message": "Repeated requests."
}'
# Separate the response body and status code
http_status8=$(echo "$Api_response8" | tail -n1)
http_body8=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status8" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status8"
else
    echo "Status code mismatch: received $http_status8, expected $expected_response_status8"
fi

# Compare the response body using jq
if [[ "$http_body8" == $expected_response_body8 ]]; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body8, expected $expected_response_body8"
fi

echo "#9. Remove Dislike First Time============================================================================="
Api_response9=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $REMOVE_DISLIKE_API_URL)
expected_response_body9='{
  "likes_count" : 0,
  "dislikes_count" : 0,
  "creator_id" : "HM",
  "description" : "learning system Design",
  "views_count" : 0,
  "default_language" : "English",
  "video_URL" : "url",
  "title" : "test for hm",
  "channel_id" : "technology",
  "video_id" : "1234567"
}'
# Separate the response body and status code
http_status9=$(echo "$Api_response9" | tail -n1)
http_body9=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status9" == $expected_response_status_200 ]]; then
    echo "Status code matches expected: $http_status9"
else
    echo "Status code mismatch: received $http_status9, expected $expected_response_status_200"
fi

# Compare the response body using jq
if echo "$http_body9" | jq --argjson expected "$expected_response_body9" '
          (.likeDislikeTableItem == $expected.likeDislikeTableItem) and
          (.videoTableItem | del(.upload_date) == $expected.videoTableItem)'; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body9, expected $expected_response_body9"
fi

echo "#10. Remove Dislike Second time==========================================================================="
Api_response10=$(curl -s -o response.json -w "%{http_code}" -X POST \
                        -H "Content-Type: application/json" \
                        -d "$API_JSON_PAYLOAD" \
                        $REMOVE_DISLIKE_API_URL)
expected_response_body10='{
  "message": "likeOrDislikeTableItem not found."
}'
# Separate the response body and status code
http_status10=$(echo "$Api_response10" | tail -n1)
http_body10=$(jq 'del(.videoTableItem.upload_date)' response.json)

# Compare the HTTP status code
if [[ "$http_status10" == $expected_response_status_404 ]]; then
    echo "Status code matches expected: $http_status10"
else
    echo "Status code mismatch: received $http_status10, expected $expected_response_status_404"
fi

# Compare the response body using jq
if [[ "$http_body10" == $expected_response_body10 ]]; then
    echo "Response body matches expected."
else
    echo "Response body does not match expected. received $http_body10, expected $expected_response_body10"
fi

# remove
aws dynamodb delete-item \
    --table-name LikeOrDislikeTable \
    --key '{"video_id": {"S": "1234567"}, "user_id": {"S": "33"}}'

aws dynamodb delete-item \
    --table-name VideoTable \
    --key '{"video_id": {"S": "1234567"}}'