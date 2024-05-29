package LambdaCronFunctions;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public class SearchVideoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private final DynamoDB dynamoDB = new DynamoDB(client);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            Table table = dynamoDB.getTable("VideoTable");
            String title = request.getQueryStringParameters().get("title");  // Required parameter
            String creator_id = request.getQueryStringParameters().get("creator_id");  // Optional parameter

            ScanSpec scanSpec = new ScanSpec().withFilterExpression("title = :v_title")
                    .withValueMap(new ValueMap().withString(":v_title", title));

            ItemCollection<ScanOutcome> items = table.scan(scanSpec);
            Iterator<Item> iterator = items.iterator();
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode videosArray = objectMapper.createArrayNode();

            while (iterator.hasNext()) {
                Item item = iterator.next();
                String itemCreator = item.getString("creator_id");
                if (creator_id == null || (itemCreator != null && itemCreator.equals(creator_id))) {
                    ObjectNode videoObject = objectMapper.createObjectNode();
                    videoObject.put("video_url", item.getString("video_URL"));  // Assuming the field is named "video_URL"
                    videosArray.add(videoObject);
                }
            }

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(videosArray));  // Convert the JSON array to string

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(videosArray));
        } catch (Exception e) {
            context.getLogger().log("Error scanning DynamoDB: " + e.getMessage());
            response.setStatusCode(500);
            response.setBody("Error processing request: " + e.getMessage());
        }
        return response;
    }
}
