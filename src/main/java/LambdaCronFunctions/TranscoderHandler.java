package LambdaCronFunctions;

import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoder;
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderClientBuilder;
import com.amazonaws.services.elastictranscoder.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TranscoderHandler implements RequestHandler<S3Event, String> {
    private static final String PIPELINE_ID = System.getenv("PIPELINE_ID");;
    
    @Override
    public String handleRequest(S3Event event, Context context) {
        LambdaLogger logger = context.getLogger();
        AmazonElasticTranscoder transcoder = AmazonElasticTranscoderClientBuilder.defaultClient();

        event.getRecords().forEach(record -> {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey().replace('+', ' ');

            JobInput input = new JobInput()
                    .withKey(key);

            CreateJobOutput output = new CreateJobOutput()
                    .withKey("transcoded-" + key)
                    .withPresetId("1351620000001-000010");  // Use appropriate preset ID for your output format

            CreateJobRequest jobRequest = new CreateJobRequest()
                    .withPipelineId(PIPELINE_ID)
                    .withInput(input)
                    .withOutput(output);

            try {
                CreateJobResult result = transcoder.createJob(jobRequest);
                logger.log("Transcoding job started for " + key + ", job ID: " + result.getJob().getId());
            } catch (AmazonElasticTranscoderException e) {
                logger.log("Failed to start transcoding job for " + key + ": " + e.getErrorMessage());
            } catch (Exception e) {
                logger.log("Failed to start transcoding job for " + key + ": " + e.getMessage());
            }
        });
        return "Success";
    }
}
