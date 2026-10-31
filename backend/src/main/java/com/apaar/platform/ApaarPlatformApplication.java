package com.apaar.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApaarPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApaarPlatformApplication.class, args);
    }
}
