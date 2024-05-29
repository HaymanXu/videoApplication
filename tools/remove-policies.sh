# remove all policies of a lambda function
# change function name before use

# Get the policy and extract statement IDs
STATEMENT_IDS=$(aws lambda get-policy --function-name youtube-demo-TranscoderFunction-O2ENfzR6QIFM | jq -r '.Policy | fromjson | .Statement[] | .Sid')

# Loop through all statement IDs and remove them
for ID in $STATEMENT_IDS; do
    aws lambda remove-permission --function-name youtube-demo-TranscoderFunction-O2ENfzR6QIFM --statement-id "$ID"
    echo "Removed $ID"
done
