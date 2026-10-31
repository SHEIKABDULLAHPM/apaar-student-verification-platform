package com.apaar.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, double threshold) {
}
