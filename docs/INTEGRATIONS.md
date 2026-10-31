# External Integrations & APAAR Adapter Boundaries

## 1. APAAR Gateway Integration Boundary

Automated Permanent Academic Account Registry (**APAAR**) is India's national student registry managed under the One Nation, One Student ID initiative.

### Architectural Port: `ApaarVerificationPort`
To insulate the core business logic from shifting external API specifications, all APAAR gateway queries must pass through `ApaarVerificationPort`.

```java
public interface ApaarVerificationPort {
    String adapterName();
    boolean authoritative();
    ApaarVerificationResult verify(ApaarVerificationRequest request);
}
```

---

## 2. Simulated Adapter (Local Development & Hackathon Demo)

Because official AICTE / NDEAR gateway credentials require accredited institutional staging access:
- The platform defaults to `SimulatedApaarVerificationAdapter`.
- **Transparency Mandate:** The adapter output explicitly sets `simulated = true` and `authoritative = false`.
- **Synthetic Test Numbers:**
  - `900000000001` - `900000000004`: Verified student demo accounts.
  - `900000000005`: Simulated non-matching identity (`SIMULATED_NO_MATCH`).
  - `900000000010`: Simulated attribute mismatch (`SIMULATED_MISMATCH`).
  - `12345`: Syntax error (less than 12 digits, rejected at format validation stage).

---

## 3. Production Gateway Integration Roadmap

When accredited credentials and Sandbox endpoints become available:
1. **mTLS Authentication:** Client certificate authentication configured in the Spring Boot `RestTemplate` / `WebClient`.
2. **AES-GCM Payload Encryption:** As specified by NDEAR / UIDAI data security guidelines.
3. **Webhook Notifications:** An asynchronous endpoint (`/api/v1/integrations/apaar/callback`) receiving batch status updates for queued verification jobs.
4. **Configuration Toggle:** Set `app.apaar.simulated-adapter.enabled=false` and provide production endpoint properties in `.env`.

