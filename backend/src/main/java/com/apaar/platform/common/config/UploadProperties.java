package com.apaar.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(long maxFileSizeBytes, List<String> allowedContentTypes) {
}
