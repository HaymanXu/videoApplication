package LambdaCronFunctions.LikeOrDislike;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class RemoveLikeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String VIDEO_TABLE = "VideoTable";
    private static final String LIKE_DISLIKE_TABLE = "LikeOrDislikeTable";
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDB = new DynamoDB(client);

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> input;
        // response set up
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        HashMap<String, String> responseBody = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            input = mapper.readValue(request.getBody(), Map.class);
        } catch (IOException e) {
            response.setStatusCode(400);
            responseBody.put("message", "Invalid input");
            response.setBody("{ \"message\": \"Invalid input provided.\" }");
            return response;
        }

        String videoId = input.get("video_id");
        String userId = input.get("user_id");
        Table likeOrDislikeTable = dynamoDB.getTable(LIKE_DISLIKE_TABLE);
        Table videoTable = dynamoDB.getTable(VIDEO_TABLE); //

        try{
            QuerySpec spec = new QuerySpec()
                    .withKeyConditionExpression("#vid = :video_id and #uid = :user_id")
                    .withNameMap(new HashMap<String, String>() {{
                        put("#vid", "video_id");
                        put("#uid", "user_id");
                    }})
                    .withValueMap(new ValueMap()
                            .withString(":video_id", videoId)
                            .withString(":user_id", userId));
            // Execute the query
            Iterator<Item> iterator = likeOrDislikeTable.query(spec).iterator();

            if (iterator.hasNext()) {
                // Define the primary key for the item you want to delete
                Map<String, AttributeValue> key = new HashMap<>();
                key.put("video_id", new AttributeValue(videoId)); // Partition Key
                key.put("user_id", new AttributeValue(userId));  // Sort Key

                // Create a DeleteItemRequest
                DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                        .withTableName(likeOrDislikeTable.getTableName())
                        .withKey(key);
                client.deleteItem(deleteItemRequest);
                Item videoTableItem = videoTable.getItem("video_id", videoId);
                int oldLikeCount = videoTableItem.getInt("likes_count");
                Item updateVideoTableItem = updateVideoTableItem(videoTable, videoId, oldLikeCount - 1);
                response.setStatusCode(200);
                response.setBody(updateVideoTableItem.toJSONPretty());
            } else {
                response.setStatusCode(404);
                response.setBody("{ \"message\": \"likeOrDislikeTableItem not found.\" }");
            }
        } catch (Exception e) {
            LambdaLogger logger = context.getLogger();
            logger.log(e.toString());
            response.setStatusCode(500);
            response.setBody("{ \"message\": \"Error processing request.\" }");
        }
        response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        return response;
    }

    public Item updateVideoTableItem(Table videoTable, String videoId, int newLikeCount) {
        UpdateItemSpec updateVideoTableItemSpec = new UpdateItemSpec()
                .withPrimaryKey("video_id", videoId)
                .withUpdateExpression("set #lk = :lk")
                .withNameMap(new NameMap().with("#lk", "likes_count"))
                .withValueMap(new ValueMap().withNumber(":lk", newLikeCount))
                .withReturnValues(ReturnValue.ALL_NEW);
        return videoTable.updateItem(updateVideoTableItemSpec).getItem();
    }
}
