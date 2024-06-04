package LambdaCronFunctions;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class PostVideoMetadataHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private final DynamoDB dynamoDB = new DynamoDB(client);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String tableName = "VideoTable";
            Table videoTable = dynamoDB.getTable(tableName);
            String jsonBody = request.getBody();
            Map<String, Object> inputData = objectMapper.readValue(jsonBody, Map.class);

            // Prepare an item with the received data and Insert the item into DynamoDB
            Item videoItem = new Item()
                    .withPrimaryKey("video_id", UUID.randomUUID().toString())
                    .withString("creator_id", (String) inputData.get("creator_id"))
                    .withString("title", (String) inputData.get("title"))
                    .withString("description", (String) inputData.get("description"))
                    .withString("upload_date", LocalDateTime.now().toString())
                    .withString("channel_id", (String) inputData.get("channel_id"))
                    .withNumber("likes_count", 0)
                    .withNumber("dislikes_count", 0)
                    .withNumber("views_count", 0)
                    .withString("video_URL", (String) inputData.get("video_URL"))
                    .withString("default_language", (String) inputData.get("default_language"));
            videoTable.putItem(videoItem);

            // Create and send a response
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(videoItem.toJSON());
            return response;

        } catch (Exception e) {
            context.getLogger().log("Error inserting item into DynamoDB: " + e.getMessage());
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(500);
            response.setBody("Error processing request");
            return response;
        }
    }
}