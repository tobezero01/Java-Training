package com.ducnhu.catalog.minio;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {
    @Bean
    public MinioClient minioClient(MinioProperties p) {
        System.out.println("MinIO props: endpoint=" + p.getEndpoint()
                + ", accessKey=" + p.getAccessKey()
                + ", secretKey=" + p.getSecretKey());
        return MinioClient.builder()
                .endpoint(p.getEndpoint())
                .credentials(p.getAccessKey(), p.getSecretKey())
                .build();
    }
}

