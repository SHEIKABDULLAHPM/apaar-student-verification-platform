package com.apaar.platform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI apaarOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("APAAR Student Governance Platform API")
                        .version("v1")
                        .description("""
                                Modular monolith API for institution data ingestion, APAAR identity resolution,
                                admission compliance, AI-assisted anomaly review, case management, academic
                                verification and AICTE reporting. All student data in this project is synthetic.
                                Simulated APAAR verification is explicitly labelled and is not authoritative.
                                """)
                        .contact(new Contact().name("SIH Project Team"))
                        .license(new License().name("Hackathon prototype - not an official AICTE service")))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
