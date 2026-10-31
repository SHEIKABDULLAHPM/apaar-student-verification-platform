package com.apaar.platform.apaar;

import com.apaar.platform.common.config.ApaarProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedApaarVerificationAdapterTest {

    private SimulatedApaarVerificationAdapter adapter;

    @BeforeEach
    void setUp() {
        ApaarProperties.Simulated simProps = new ApaarProperties.Simulated(true, "simulated-apaar-adapter");
        ApaarProperties properties = new ApaarProperties(simProps, null);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);
        adapter = new SimulatedApaarVerificationAdapter(properties, fixedClock);
    }

    @Test
    @DisplayName("Simulated adapter is explicitly labelled non-authoritative")
    void nonAuthoritativeLabel() {
        assertThat(adapter.authoritative()).isFalse();
        assertThat(adapter.adapterName()).isEqualTo("simulated-apaar-adapter");
    }

    @Test
    @DisplayName("Deterministic demo rule: digits ending with 0 produce MISMATCH")
    void mismatchRule() {
        ApaarVerificationPort.ApaarVerificationRequest req = new ApaarVerificationPort.ApaarVerificationRequest(
                UUID.randomUUID(), "900000000010", "Synthetic Student", LocalDate.of(2005, 1, 1)
        );
        ApaarVerificationPort.ApaarVerificationResult res = adapter.verify(req);
        assertThat(res.status()).isEqualTo("MISMATCH");
        assertThat(res.simulated()).isTrue();
        assertThat(res.authoritative()).isFalse();
        assertThat(res.reasonCode()).isEqualTo("SIMULATED_MISMATCH");
    }

    @Test
    @DisplayName("Deterministic demo rule: digits ending with 5 produce NOT_VERIFIED")
    void notVerifiedRule() {
        ApaarVerificationPort.ApaarVerificationRequest req = new ApaarVerificationPort.ApaarVerificationRequest(
                UUID.randomUUID(), "900000000005", "Synthetic Student", LocalDate.of(2005, 1, 1)
        );
        ApaarVerificationPort.ApaarVerificationResult res = adapter.verify(req);
        assertThat(res.status()).isEqualTo("NOT_VERIFIED");
        assertThat(res.simulated()).isTrue();
        assertThat(res.authoritative()).isFalse();
        assertThat(res.reasonCode()).isEqualTo("SIMULATED_NO_MATCH");
    }

    @Test
    @DisplayName("Deterministic demo rule: other digits produce VERIFIED")
    void verifiedRule() {
        ApaarVerificationPort.ApaarVerificationRequest req = new ApaarVerificationPort.ApaarVerificationRequest(
                UUID.randomUUID(), "900000000001", "Synthetic Student", LocalDate.of(2005, 1, 1)
        );
        ApaarVerificationPort.ApaarVerificationResult res = adapter.verify(req);
        assertThat(res.status()).isEqualTo("VERIFIED");
        assertThat(res.simulated()).isTrue();
        assertThat(res.authoritative()).isFalse();
        assertThat(res.reasonCode()).isEqualTo("SIMULATED_MATCH");
    }
}
