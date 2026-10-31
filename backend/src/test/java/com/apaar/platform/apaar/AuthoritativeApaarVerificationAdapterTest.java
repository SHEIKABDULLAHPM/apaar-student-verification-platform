package com.apaar.platform.apaar;

import com.apaar.platform.common.config.ApaarProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthoritativeApaarVerificationAdapterTest {

    @Test
    @DisplayName("Authoritative adapter is marked authoritative and non-simulated")
    void authoritativeLabel() {
        ApaarProperties props = new ApaarProperties(
                new ApaarProperties.Simulated(true, "sim-adapter"),
                new ApaarProperties.Authoritative(false, "auth-adapter", null, 5)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC);
        AuthoritativeApaarVerificationAdapter adapter = new AuthoritativeApaarVerificationAdapter(props, clock);

        assertThat(adapter.authoritative()).isTrue();
        assertThat(adapter.adapterName()).isEqualTo("auth-adapter");
    }

    @Test
    @DisplayName("When gateway unconfigured, returns PROVIDER_UNAVAILABLE safely without synthetic fabrication")
    void providerUnavailableWhenUnconfigured() {
        ApaarProperties props = new ApaarProperties(
                new ApaarProperties.Simulated(true, "sim-adapter"),
                new ApaarProperties.Authoritative(false, "authoritative-gateway", null, 5)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC);
        AuthoritativeApaarVerificationAdapter adapter = new AuthoritativeApaarVerificationAdapter(props, clock);

        ApaarVerificationPort.ApaarVerificationRequest req = new ApaarVerificationPort.ApaarVerificationRequest(
                UUID.randomUUID(), "900000000001", "Aarav Sharma", LocalDate.of(2005, 5, 12)
        );

        ApaarVerificationPort.ApaarVerificationResult result = adapter.verify(req);

        assertThat(result.status()).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(result.reasonCode()).isEqualTo("GATEWAY_NOT_CONFIGURED");
        assertThat(result.authoritative()).isTrue();
        assertThat(result.simulated()).isFalse();
        assertThat(result.payload()).containsKey("error");
    }

    @Test
    @DisplayName("When gateway configured with endpoint but fails connection, returns VERIFICATION_FAILED safely")
    void connectionTimeoutHandledSafely() {
        ApaarProperties props = new ApaarProperties(
                new ApaarProperties.Simulated(true, "sim-adapter"),
                new ApaarProperties.Authoritative(true, "authoritative-gateway", "https://api.apaar.gov.in/v1/verify", 5)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC);
        AuthoritativeApaarVerificationAdapter adapter = new AuthoritativeApaarVerificationAdapter(props, clock);

        ApaarVerificationPort.ApaarVerificationRequest req = new ApaarVerificationPort.ApaarVerificationRequest(
                UUID.randomUUID(), "900000000001", "Aarav Sharma", LocalDate.of(2005, 5, 12)
        );

        ApaarVerificationPort.ApaarVerificationResult result = adapter.verify(req);

        assertThat(result.status()).isEqualTo("VERIFICATION_FAILED");
        assertThat(result.reasonCode()).isEqualTo("CONNECTION_TIMEOUT");
        assertThat(result.authoritative()).isTrue();
        assertThat(result.simulated()).isFalse();
    }
}
