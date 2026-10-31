# APAAR Verification Adapter Interface & Specifications

## 1. Architectural Boundary

The APAAR Platform defines a strict hexagonal architecture port for APAAR Identity Resolution:
`com.apaar.platform.apaar.ApaarVerificationPort`.

```text
                  +-----------------------------------+
                  |  Institutional Ingestion / Admin  |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------------------------+
                  |      ApaarVerificationService      |
                  +-----------------+-----------------+
                                    |
                    ApaarVerificationPort (Java Interface)
                                    |
           +------------------------+------------------------+
           |                                                 |
+----------v-----------+                          +----------v-----------+
|  Simulated Adapter   |                          |  Authorized Adapter  |
| (Demo / Synthetic)   |                          |  (NDEAR / DigiLocker)|
+----------------------+                          +----------------------+
```

### Critical Separation: Format Validation vs Authoritative Verification
1. **Format Validation (`ApaarFormatValidator`):**
   - Purely structural syntax check (exactly 12 numeric digits; no checksum is assumed).
   - Can run locally and offline during bulk ingestion.
   - **Does NOT prove that the student identity exists or is active.**

2. **Authoritative Verification (`ApaarVerificationPort`):**
   - Interacts with the authorized government / NDEAR / DigiLocker APAAR Gateway.
   - Matches APAAR ID against authoritative student registry attributes: Student Legal Name, Date of Birth, Gender, Father/Mother/Guardian name.
   - Returns cryptographically verifiable match status or reason-coded mismatch.

---

## 2. Simulated Verification Adapter (Demo Environment)

For local development and demonstrations where official AICTE/NDEAR sandbox credentials are not available, the platform defaults to `SimulatedApaarVerificationAdapter`.

- **Configuration:** `app.apaar.simulated-adapter.enabled=true`
- **Audit Marker:** All records issued by this adapter persist `verification_mode = 'SIMULATED'` and `authoritative = false`.
- **Deterministic Rules (Demo Only):**
  - Identifier ending in `0` -> `MISMATCH` (`SIMULATED_MISMATCH`)
  - Identifier ending in `5` -> `NOT_VERIFIED` (`SIMULATED_NO_MATCH`)
  - Any other ending digit -> `VERIFIED` (`SIMULATED_MATCH`)

> [!WARNING]
> Simulated verification must NEVER be presented to users or auditors as authoritative APAAR verification. The platform UI always displays a `SIMULATED` indicator tag on mock verification records.

---

## 3. Production Adapter Specification (Interface Contract)

Any real adapter implementation must fulfill the following contract:

### Request Structure
```json
{
  "studentId": "10000000-0000-4000-8000-000000000001",
  "apaarId": "900000000001",
  "fullName": "Aarav Sharma",
  "dateOfBirth": "2007-03-14",
  "gender": "MALE"
}
```

### Response Structure
```json
{
  "status": "VERIFIED | MISMATCH | NOT_VERIFIED | FAILED",
  "reasonCode": "MATCH_CONFIRMED | NAME_DOB_MISMATCH | ID_NOT_FOUND | GATEWAY_TIMEOUT",
  "providerReference": "GOV-TXN-98234-2026",
  "simulated": false,
  "authoritative": true,
  "message": "Identity resolved and verified against central registry.",
  "verifiedAt": "2026-10-30T14:20:00Z",
  "expiresAt": "2027-10-30T14:20:00Z",
  "payload": {
    "signedToken": "...",
    "matchConfidence": 1.0
  }
}
```

