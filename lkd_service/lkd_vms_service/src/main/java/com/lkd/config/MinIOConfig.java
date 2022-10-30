package com.lkd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类,
 */
@Configuration
@ConfigurationProperties("minio")//配置绑定
@Data
public class MinIOConfig {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String endpoint;
    private String readPath;
}
