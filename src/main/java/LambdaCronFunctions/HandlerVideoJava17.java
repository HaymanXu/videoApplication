package LambdaCronFunctions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HandlerVideoJava17 implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String bucketName = request.getPathParameters().get("bucketName");
        String fileName = request.getPathParameters().get("fileName");
        String httpMethod = request.getHttpMethod();
        S3Client s3 = S3Client.builder()
                .region(Region.US_WEST_1)
                .httpClient(ApacheHttpClient.builder().build())
                .build();

        try {
            switch (httpMethod) {
                case "POST":
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
                    return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("File uploaded successfully!");

                case "GET":
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
                    String base64EncodedContent = java.util.Base64.getEncoder().encodeToString(content);

                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", getContentType);  // Use the actual content type
                    System.out.println("getContentType from s3.getObject(getObjectRequest): " + getContentType);
                    System.out.println("Get complete. ETag: " + getResponse.eTag());
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(200)
                            .withHeaders(headers)
                            .withBody(base64EncodedContent)
                            .withIsBase64Encoded(true);

                case "DELETE":
                    DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .build();
                    s3.deleteObject(deleteObjectRequest);
                    return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("File deleted successfully!");
                default:
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(405)
                            .withBody("Method Not Allowed");
            }
        } catch (Exception e) {
            context.getLogger().log("Error handling request: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Server Error" + e.getMessage());
        }
    }
}
