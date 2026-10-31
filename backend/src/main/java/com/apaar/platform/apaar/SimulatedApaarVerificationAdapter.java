package com.apaar.platform.apaar;

import com.apaar.platform.common.config.ApaarProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic simulated adapter for demonstrations ONLY.
 * It never accesses the real APAAR system and its result is labelled SIMULATED.
 *
 * Deterministic rule: last digit 0 => MISMATCH, last digit 5 => NOT_VERIFIED,
 * otherwise VERIFIED. This is a demo behaviour, not an APAAR specification.
 */
@Component("simulatedApaarVerificationAdapter")
@org.springframework.context.annotation.Primary
@ConditionalOnProperty(name = "app.apaar.simulated-adapter.enabled", havingValue = "true", matchIfMissing = true)
public class SimulatedApaarVerificationAdapter implements ApaarVerificationPort {
    private final ApaarProperties properties;
    private final Clock clock;

    public SimulatedApaarVerificationAdapter(ApaarProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String adapterName() {
        return properties.simulatedAdapter().adapterName();
    }

    @Override
    public boolean authoritative() {
        return false;
    }

    @Override
    public ApaarVerificationResult verify(ApaarVerificationRequest request) {
        char last = request.apaarId().charAt(request.apaarId().length() - 1);
        String status = last == '0' ? "MISMATCH" : last == '5' ? "NOT_VERIFIED" : "VERIFIED";
        String reason = last == '0' ? "SIMULATED_MISMATCH" : last == '5' ? "SIMULATED_NO_MATCH" : "SIMULATED_MATCH";
        Instant now = clock.instant();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("simulated", true);
        payload.put("authoritative", false);
        payload.put("adapter", adapterName());
        payload.put("disclaimer", "Synthetic demo result. This is NOT real APAAR verification.");
        payload.put("rule", "last-digit deterministic demo rule");
        return new ApaarVerificationResult(status, reason, "SIM-" + UUID.randomUUID(), true, false,
                "Simulated verification result - not authoritative", now, now.plusSeconds(365L * 24 * 3600), payload);
    }
}
