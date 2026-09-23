package com.tutorplatform.file.infrastructure;

import com.tutorplatform.file.application.FileStorage;
import java.net.URI;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration(proxyBeanMethods = false)
public class FileStorageConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.file-storage", name = "provider", havingValue = "S3")
    S3Client s3Client(
            @Value("${app.file-storage.s3.endpoint}") String endpoint,
            @Value("${app.file-storage.s3.region}") String region) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    FileStorage fileStorage(
            @Value("${app.file-storage.provider:LOCAL}") String writeProvider,
            @Value("${app.file-storage.directory:./var/files}") String localDirectory,
            @Value("${app.file-storage.s3.bucket:}") String s3Bucket,
            ObjectProvider<S3Client> s3Clients) {
        FileStorage local = new LocalFileStorage(localDirectory);
        S3Client client = s3Clients.getIfAvailable();
        FileStorage remote = client == null ? null : new S3FileStorage(client, s3Bucket);
        return new RoutingFileStorage(local, remote, writeProvider);
    }
}
