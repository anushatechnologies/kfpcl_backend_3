package com.project.kfpcl_exports.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class AwsS3Config {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    @Value("${aws.region:ap-south-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket:kfpcl-exports-media}")
    private String s3Bucket;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.accessKeyId:}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey:}")
    private String secretAccessKey;

    @Bean
    public S3Client s3Client() {
        log.info("Initializing S3Client with region: '{}', bucket: '{}', endpoint: '{}'", awsRegion, s3Bucket, s3Endpoint);
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(awsRegion));

        if (StringUtils.hasText(s3Endpoint)) {
            builder.endpointOverride(URI.create(s3Endpoint))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        log.info("Initializing S3Presigner with region: '{}'", awsRegion);
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(awsRegion));

        if (StringUtils.hasText(s3Endpoint)) {
            builder.endpointOverride(URI.create(s3Endpoint))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getAwsRegion() {
        return awsRegion;
    }
}

