package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.config.AwsS3Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:livetestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class SupabaseStorageLiveTest {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private AwsS3Config awsS3Config;

    @Test
    void testLiveSupabaseS3Integration() {
        String bucket = awsS3Config.getS3Bucket();
        assertNotNull(bucket, "Bucket name must be configured");
        System.out.println("Testing connection to Supabase bucket: " + bucket);

        // Test HeadBucket or ListObjects
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucket).build();
            s3Client.headBucket(headBucketRequest);
            System.out.println("Bucket exists and is accessible: " + bucket);
        } catch (Exception e) {
            System.out.println("headBucket notice: " + e.getMessage());
        }

        String testKey = "loan-applications/test-verify/connectivity-test-" + UUID.randomUUID() + ".txt";
        byte[] testBytes = "Hello Supabase Storage S3 connectivity test".getBytes(StandardCharsets.UTF_8);

        // Put test object
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(testKey)
                .contentType("text/plain")
                .contentLength((long) testBytes.length)
                .build();
        s3Client.putObject(putReq, RequestBody.fromBytes(testBytes));
        System.out.println("Successfully uploaded test object to Supabase bucket: " + testKey);

        // Get test object
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(testKey)
                .build();
        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(getReq)) {
            byte[] downloaded = stream.readAllBytes();
            assertEquals("Hello Supabase Storage S3 connectivity test", new String(downloaded, StandardCharsets.UTF_8));
            System.out.println("Successfully downloaded and verified test object from Supabase bucket!");
        } catch (Exception e) {
            fail("Failed to download object: " + e.getMessage());
        }

        // Clean up
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(testKey)
                .build();
        s3Client.deleteObject(delReq);
        System.out.println("Successfully cleaned up test object.");
    }
}
