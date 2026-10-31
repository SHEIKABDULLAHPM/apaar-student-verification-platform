package com.apaar.platform.common.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Normalises PostgreSQL connection URLs supplied by hosting platforms.
 *
 * <p>Render, Heroku, and similar platforms inject a connection string such as
 * {@code postgres://user:password@host:5432/database}. Spring's datasource
 * auto-configuration only understands JDBC URLs, so without this translation the
 * application either ignores the injected URL or fails with an unsupported
 * scheme error.
 *
 * <p>The processor accepts {@code SPRING_DATASOURCE_URL}, {@code DATABASE_URL},
 * or {@code DB_URL}. Explicit {@code SPRING_DATASOURCE_USERNAME} and
 * {@code SPRING_DATASOURCE_PASSWORD} values always win over credentials embedded
 * in the URL.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DATASOURCE_URL = "spring.datasource.url";
    static final String DATASOURCE_USERNAME = "spring.datasource.username";
    static final String DATASOURCE_PASSWORD = "spring.datasource.password";

    private static final String[] URL_PROPERTIES = {
        "SPRING_DATASOURCE_URL", "DATABASE_URL", "DB_URL"
    };

    private static final String PROPERTY_SOURCE_NAME = "apaarDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = firstNonBlank(environment, URL_PROPERTIES);
        if (rawUrl == null) {
            return;
        }

        String trimmed = rawUrl.trim();
        if (!isPostgresUrl(trimmed)) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                "Invalid PostgreSQL connection URL: " + redact(trimmed), ex);
        }

        String authority = uri.getRawAuthority();
        String userInfo = uri.getRawUserInfo();
        String path = uri.getRawPath();
        if (authority == null || path == null || path.length() <= 1) {
            throw new IllegalStateException(
                "PostgreSQL connection URL must include a host and database name: "
                    + redact(trimmed));
        }

        String jdbcAuthority = userInfo == null ? authority : authority.substring(userInfo.length() + 1);
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
            .append(jdbcAuthority)
            .append(path);
        if (uri.getRawQuery() != null) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(DATASOURCE_URL, jdbcUrl.toString());

        if (userInfo != null) {
            String[] credentials = userInfo.split(":", 2);
            if (isBlank(environment.getProperty("SPRING_DATASOURCE_USERNAME"))) {
                overrides.put(DATASOURCE_USERNAME, decode(credentials[0]));
            }
            if (credentials.length > 1
                && isBlank(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
                overrides.put(DATASOURCE_PASSWORD, decode(credentials[1]));
            }
        }

        environment.getPropertySources()
            .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }

    @Override
    public int getOrder() {
        // Run after ConfigDataEnvironmentPostProcessor so imported .env values and
        // platform environment variables are already visible.
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static boolean isPostgresUrl(String url) {
        return url.startsWith("postgres://") || url.startsWith("postgresql://");
    }

    private static String firstNonBlank(ConfigurableEnvironment environment, String... names) {
        for (String name : names) {
            String value = environment.getProperty(name);
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String redact(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }
        int at = url.indexOf('@', schemeEnd + 3);
        if (at < 0) {
            return url;
        }
        return url.substring(0, schemeEnd + 3) + "***" + url.substring(at);
    }
}
