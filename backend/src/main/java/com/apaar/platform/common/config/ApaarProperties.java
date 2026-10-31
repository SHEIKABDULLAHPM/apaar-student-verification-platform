package com.apaar.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.apaar")
public record ApaarProperties(Simulated simulatedAdapter, Authoritative authoritativeAdapter) {
    public record Simulated(boolean enabled, String adapterName) {
    }

    public record Authoritative(boolean enabled, String adapterName, String endpointUrl, int timeoutSeconds) {
        public Authoritative {
            if (adapterName == null) adapterName = "authoritative-apaar-gateway-adapter";
            if (timeoutSeconds <= 0) timeoutSeconds = 5;
        }
    }
}
