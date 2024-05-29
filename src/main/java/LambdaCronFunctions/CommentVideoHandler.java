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

public class CommentVideoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private final DynamoDB dynamoDB = new DynamoDB(client);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String tableName = "CommentsTable";
            Table commentsTable = dynamoDB.getTable(tableName);
            String jsonBody = request.getBody();
            Map<String, Object> inputData = objectMapper.readValue(jsonBody, Map.class);

            // Prepare an item with the received data and Insert the item into DynamoDB
            Item commentsItem = new Item()
                    .withPrimaryKey("comment_id", UUID.randomUUID().toString())
                    .withString("video_id", (String) inputData.get("video_id"))
                    .withString("user_id", (String) inputData.get("user_id"))
                    .withString("date_posted", LocalDateTime.now().toString())
                    .withString("comment_text", (String) inputData.get("comment_text"));
            commentsTable.putItem(commentsItem);

            // Create and send a response
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(commentsItem.toJSON());
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