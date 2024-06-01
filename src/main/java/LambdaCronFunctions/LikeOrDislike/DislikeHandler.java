package LambdaCronFunctions.LikeOrDislike;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
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

public class DislikeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String VIDEO_TABLE = "VideoTable";
    private static final String LIKE_DISLIKE_TABLE = "LikeOrDislikeTable";
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDB = new DynamoDB(client);

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
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

        try {
            // get likeOrDislikeTableItem based on video_id and user_id
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

            Item videoTableItem = videoTable.getItem("video_id", videoId);

            int oldLikeCount = videoTableItem.getInt("likes_count");
            int oldDislikeCount = videoTableItem.getInt("dislikes_count");

            if (iterator.hasNext()) {
                Item likeOrDislikeTableItem = iterator.next();
                int oldLikeValue = likeOrDislikeTableItem.getInt("like");
                //like --> dislike
                if (oldLikeValue == 1) {
                    UpdateItemSpec updateLikeOrDislikeTableItemSpec = new UpdateItemSpec()
                            .withPrimaryKey("video_id", videoId, "user_id", userId)
                            .withUpdateExpression("set #lk = :lk")
                            .withNameMap(new NameMap().with("#lk", "like"))
                            .withValueMap(new ValueMap().withNumber(":lk", -1))
                            .withReturnValues(ReturnValue.ALL_NEW);

                    Item updateLikeOrDislikeTableItem = likeOrDislikeTable.updateItem(updateLikeOrDislikeTableItemSpec).getItem();
                    logger.log(updateLikeOrDislikeTableItem.toJSONPretty());
                    Item updateVideoTableItem = updateVideoTableItem(videoTable, videoId, oldLikeCount - 1, oldDislikeCount + 1);
                    logger.log(updateVideoTableItem.toJSONPretty());
                    String combinedResponse = String.format("{\"likeDislikeTableItem\": %s, \"videoTableItem\": %s}", updateLikeOrDislikeTableItem.toJSONPretty(), updateVideoTableItem.toJSONPretty());

                    response.setStatusCode(200);
                    response.setBody(combinedResponse);
                } else {
                    // existing same record
                    response.setStatusCode(200);
                    response.setBody("{ \"message\": \"Repeated requests.\" }");
                }
            } else {
                // No item: add new record in likeDislikeTable
                Item newlikeDislikeTableItem = new Item()
                        .withPrimaryKey("video_id", videoId, "user_id", userId)
                        .withNumber("like", -1);
                likeOrDislikeTable.putItem(newlikeDislikeTableItem);
                Item updateVideoTableItem = updateVideoTableItem(videoTable, videoId, oldLikeCount, oldDislikeCount + 1);

                String combinedResponse = String.format("{\"likeDislikeTableItem\": %s, \"videoTableItem\": %s}", newlikeDislikeTableItem.toJSONPretty(), updateVideoTableItem.toJSONPretty());

                response.setStatusCode(200);
                response.setBody(combinedResponse);
            }
        } catch (Exception e) {
            logger.log(e.toString());
            response.setStatusCode(500);
            response.setBody("{ \"message\": \"Error processing request.\" }");
        }
        response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        return response;
    }

    public Item updateVideoTableItem(Table videoTable, String videoId, int newLikeCount, int newDislikeCount) {
        UpdateItemSpec updateVideoTableItemSpec = new UpdateItemSpec()
                .withPrimaryKey("video_id", videoId)
                .withUpdateExpression("set #lk = :lk, #lu = :lu")
                .withNameMap(new NameMap()
                        .with("#lk", "likes_count")
                        .with("#lu", "dislikes_count"))
                .withValueMap(new ValueMap()
                        .withNumber(":lk", newLikeCount)
                        .withNumber(":lu", newDislikeCount))
                .withReturnValues(ReturnValue.ALL_NEW);
        return videoTable.updateItem(updateVideoTableItemSpec).getItem();
    }
}