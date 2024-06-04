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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
                String bucketName = item.getString("bucket_name");
                context.getLogger().log("BucketName: " + bucketName);
                String fileName = item.getString("file_name");
                context.getLogger().log("FileName: " + fileName);

                Date expiration = new Date();
                long expTimeMillis = expiration.getTime();
                expTimeMillis += 1000 * 60 * 60; // 1 hour
                expiration.setTime(expTimeMillis);

                GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
                        .withMethod(com.amazonaws.HttpMethod.GET)
                        .withExpiration(expiration);
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
                URL presignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");  // Allow all origins or specify a specific origin
                headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
                response.setStatusCode(200);
                response.setHeaders(headers);
                Map<String, Object> itemMap = item.asMap();
                itemMap.put("presignedUrl", presignedUrl.toString());
                context.getLogger().log("itemMap: " + itemMap.toString());
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse;
                try {
                    jsonResponse = objectMapper.writeValueAsString(itemMap);
                } catch (Exception e) {
                    throw new RuntimeException("Error converting response to JSON", e);
                }

                response.setBody(jsonResponse);
            }
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
        return response;
    }
}