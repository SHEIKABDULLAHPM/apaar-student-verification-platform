package com.apaar.platform.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor =
        new DatabaseUrlEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void convertsRenderStyleUrlAndExtractsCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(
            "DATABASE_URL",
            "postgres://apaar_user:p%40ss@dpg-abc-a:5432/apaar_x7k2?sslmode=require");

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_URL))
            .isEqualTo("jdbc:postgresql://dpg-abc-a:5432/apaar_x7k2?sslmode=require");
        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_USERNAME))
            .isEqualTo("apaar_user");
        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_PASSWORD))
            .isEqualTo("p@ss");
    }

    @Test
    void acceptsPostgresqlSchemeWithoutCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_URL", "postgresql://db.internal:5432/apaar");

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_URL))
            .isEqualTo("jdbc:postgresql://db.internal:5432/apaar");
        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_USERNAME))
            .isNull();
    }

    @Test
    void explicitCredentialsWinOverUrlCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "postgres://url-user:url-pass@db:5432/apaar");
        environment.setProperty("SPRING_DATASOURCE_USERNAME", "explicit-user");
        environment.setProperty("SPRING_DATASOURCE_PASSWORD", "explicit-pass");

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_URL))
            .isEqualTo("jdbc:postgresql://db:5432/apaar");
        assertThat(environment.getProperty("SPRING_DATASOURCE_USERNAME"))
            .isEqualTo("explicit-user");
        assertThat(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))
            .isEqualTo("explicit-pass");
        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_USERNAME))
            .isNull();
        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_PASSWORD))
            .isNull();
    }

    @Test
    void leavesJdbcUrlUntouched() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(
            DatabaseUrlEnvironmentPostProcessor.DATASOURCE_URL,
            "jdbc:postgresql://localhost:5432/apaar");

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty(DatabaseUrlEnvironmentPostProcessor.DATASOURCE_URL))
            .isEqualTo("jdbc:postgresql://localhost:5432/apaar");
    }

    @Test
    void rejectsUrlWithoutDatabaseName() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "postgres://user:pass@db:5432");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, application))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must include a host and database name");
    }
}
