package LambdaCronFunctions;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class GetVideoMetadataHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String video_id = request.getPathParameters().get("videoId");
            Table table = dynamoDB.getTable("VideoTable");
            context.getLogger().log("Search with PrimaryKey: " + video_id);
            GetItemSpec spec = new GetItemSpec().withPrimaryKey("video_id", video_id);
            Item item = table.getItem(spec);

            if (item == null) {
                response.setStatusCode(404);
                response.setBody("{\"error\":\"Item not found\"}");
            } else {
                response.setStatusCode(200);
                response.setBody(item.toJSON());
            }
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
        return response;
    }
}