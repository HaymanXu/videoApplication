package LambdaCronFunctions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UploadVideoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String bucketName = request.getPathParameters().get("bucketName");
        String fileName = request.getPathParameters().get("fileName");
        S3Client s3 = S3Client.builder()
                .region(Region.US_WEST_1)
                .httpClient(ApacheHttpClient.builder().build())
                .build();

        try {
            String file = request.getBody();
            byte[] fileContent = Base64.getDecoder().decode(file);
            RequestBody requestBody = RequestBody.fromBytes(fileContent);
            String postContentType = request.getHeaders().getOrDefault("Content-Type", "application/octet-stream");
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(postContentType)
                    .build();

            PutObjectResponse putResponse = s3.putObject(putObjectRequest, requestBody);
            System.out.println("Upload complete. ETag: " + putResponse.eTag());
            System.out.println("postContentType: " + postContentType);
            // Construct the S3 Object URL
            String region = "us-west-1"; // Ensure this matches the region you're using
            String objectUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);

            // Create response body map
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("Object Url", objectUrl);

            // Convert map to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponseBody = objectMapper.writeValueAsString(responseBody);
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(Map.of("Content-Type", "application/json"));
            response.setBody(jsonResponseBody);
            return response;
        } catch (Exception e) {
            context.getLogger().log("Error handling request: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Server Error" + e.getMessage());
        }
    }
}
