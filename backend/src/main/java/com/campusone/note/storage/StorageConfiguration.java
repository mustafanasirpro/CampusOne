package com.campusone.note.storage;

import jakarta.servlet.MultipartConfigElement;
import java.net.URI;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean(destroyMethod = "close")
    StorageService storageService(
            StorageProperties properties,
            Clock clock) {
        if (!properties.isR2Configured()) {
            if (properties.isR2Requested()) {
                LOGGER.warn(
                        "R2 storage was requested but its configuration is incomplete; "
                                + "file upload remains disabled.");
            }
            return new DisabledStorageService();
        }

        try {
            StorageProperties.R2 r2 = properties.getR2();
            URI endpoint = requireHttpEndpoint(r2.getEndpoint());
            if (!r2.getPublicBaseUrl().isBlank()) {
                requireHttpEndpoint(r2.getPublicBaseUrl());
            }
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    r2.getAccessKeyId(),
                    r2.getSecretAccessKey());
            StaticCredentialsProvider credentialsProvider =
                    StaticCredentialsProvider.create(credentials);
            S3Configuration serviceConfiguration = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .chunkedEncodingEnabled(false)
                    .build();
            S3Client client = S3Client.builder()
                    .endpointOverride(endpoint)
                    .credentialsProvider(credentialsProvider)
                    .region(Region.of(r2.getRegion()))
                    .serviceConfiguration(serviceConfiguration)
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build();
            S3Presigner presigner = S3Presigner.builder()
                    .endpointOverride(endpoint)
                    .credentialsProvider(credentialsProvider)
                    .region(Region.of(r2.getRegion()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
            return new R2StorageService(client, presigner, properties, clock);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn(
                    "R2 storage configuration is invalid; file upload remains disabled.");
            return new DisabledStorageService();
        }
    }

    @Bean
    MultipartConfigElement multipartConfigElement(
            StorageProperties properties) {
        DataSize maximumFileSize =
                DataSize.ofMegabytes(Math.max(
                        properties.getMaxUploadSizeMb(),
                        properties.getMarketplaceMaxImageSizeMb()));
        DataSize maximumRequestSize = DataSize.ofBytes(
                Math.max(
                        DataSize.ofMegabytes(properties.getMaxUploadSizeMb())
                                .toBytes(),
                        DataSize.ofMegabytes(
                                (long) properties.getMarketplaceMaxImageSizeMb()
                                        * properties.getMarketplaceMaxImagesPerListing())
                                .toBytes())
                        + DataSize.ofMegabytes(1).toBytes());
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(maximumFileSize);
        factory.setMaxRequestSize(maximumRequestSize);
        return factory.createMultipartConfig();
    }

    private URI requireHttpEndpoint(String value) {
        URI endpoint = URI.create(value);
        if (endpoint.getHost() == null
                || (!"https".equalsIgnoreCase(endpoint.getScheme())
                && !"http".equalsIgnoreCase(endpoint.getScheme()))) {
            throw new IllegalArgumentException("R2 endpoint must be an HTTP(S) URL.");
        }
        return endpoint;
    }
}
