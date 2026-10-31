package com.apaar.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.login")
public record LoginProperties(int maxFailedAttempts, Duration lockDuration) {
}
