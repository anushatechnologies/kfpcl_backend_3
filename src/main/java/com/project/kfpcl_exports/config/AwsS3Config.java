package com.project.kfpcl_exports.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    @Value("${aws.region:ap-south-2}")
    private String awsRegion;

    @Value("${aws.s3.bucket:kfpcl-exports-media-319759856065}")
    private String s3Bucket;

    @Bean
    public S3Client s3Client() {
        log.info("Initializing AWS S3Client with region: '{}', bucket: '{}'", awsRegion, s3Bucket);
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        log.info("Initializing AWS S3Presigner with region: '{}'", awsRegion);
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getAwsRegion() {
        return awsRegion;
    }
}
