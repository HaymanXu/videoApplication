package LambdaCronFunctions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class StreamVideoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String bucketName = request.getPathParameters().get("bucketName");
        String fileName = request.getPathParameters().get("fileName");
        S3Client s3 = S3Client.builder()
                .region(Region.US_WEST_1)
                .httpClient(ApacheHttpClient.builder().build())
                .build();

        try {
            // Creating a request to retrieve a object from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            ResponseInputStream<GetObjectResponse> object = s3.getObject(getObjectRequest);
            GetObjectResponse getResponse = object.response();

            // Get content type from S3 object metadata
            String getContentType = getResponse.contentType();

            // Read the content as bytes, then encode as base64 for safe transmission over JSON
            byte[] content = object.readAllBytes();
            String base64EncodedContent = Base64.getEncoder().encodeToString(content);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", getContentType);  // Use the actual content type
            System.out.println("getContentType from s3.getObject(getObjectRequest): " + getContentType);
            System.out.println("Get complete. ETag: " + getResponse.eTag());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(base64EncodedContent)
                    .withIsBase64Encoded(true);
        } catch (Exception e) {
            context.getLogger().log("Error handling request: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Server Error" + e.getMessage());
        }
    }
}