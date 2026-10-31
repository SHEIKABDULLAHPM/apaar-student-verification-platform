# Security, Authentication, and Role-Based Access Control (RBAC)

## 1. Security Architecture Principles

1. **Defense in Depth:** Security enforcement occurs at the gateway (Nginx), HTTP transport (TLS / CORS), application layer (Spring Security filters), business service layer (`AccessScopeService`), and persistence layer (PostgreSQL triggers).
2. **Server-Side Authorization Authority:** Frontend UI route gates and visibility helpers are purely aesthetic. All access control, tenant boundary checks, and capability permissions are verified on the server for every incoming request.
3. **Tenant & Institution Isolation:** Institutional staff are strictly scoped to their own institution. Attempting to query or modify another institution's identifier throws a `403 Forbidden` (`INSTITUTION_ACCESS_DENIED`).
4. **Least Privilege:** Granular permission codes are bound to system roles. Administrative superuser privileges are constrained to `PLATFORM_ADMIN`.

---

## 2. System Roles and Responsibility Matrix

The platform implements the 7 mandatory regulatory roles:

| Role Code | Role Name | Scope | Primary Capabilities |
| :--- | :--- | :--- | :--- |
| `PLATFORM_ADMIN` | Platform Administrator | Global | System configuration, user provisioning, retention policies, global overrides. |
| `AICTE_OFFICER` | AICTE Regulatory Officer | Global | National oversight, intake vs enrolment compliance audits, anomaly triage, case review, report export. |
| `INSTITUTION_ADMIN` | Institution Administrator | Institution | Manage institution profile, programmes, intake quotas, bulk data submission, initiate compliance self-checks. |
| `INSTITUTION_OPERATOR` | Institution Data Operator | Institution | Upload student roster files, resolve row-level import errors, update student admissions. |
| `VERIFICATION_OFFICER` | Verification Officer | Institution | Issue academic verification tokens, generate QR reference codes, review credential logs. |
| `CASE_REVIEWER` | Case Reviewer | Global / Inst | Investigate cross-institution identity conflicts, collect evidence, draft case resolution recommendations. |
| `AUDITOR` | Independent Regulatory Auditor | Global (Read-only) | Full read-only audit visibility into all historical data mutations, decisions, and system logs. |

---

## 3. Institution Data Isolation: `AccessScopeService`

All sensitive operations route through `AccessScopeService`:

```java
// Example usage in InstitutionService, EnrolmentService, ImportService:
UUID effectiveInstitutionId = accessScopeService.resolveInstitutionFilter(requestedInstitutionId);
```

### Isolation Logic:
- If actor has `PLATFORM_ADMIN`, `AICTE_OFFICER`, `AUDITOR`, or `CASE_REVIEWER`, `isPlatformWide()` returns `true`, permitting cross-institution filtering.
- For `INSTITUTION_ADMIN` or `INSTITUTION_OPERATOR`:
  - If `requestedInstitutionId` is null, the query is automatically clamped to `principal.getInstitutionId()`.
  - If `requestedInstitutionId` is supplied but does not match `principal.getInstitutionId()`, a `ForbiddenOperationException("INSTITUTION_ACCESS_DENIED")` is thrown immediately.

---

## 4. Authentication & Token Management

- **Local Password Authentication:** BCrypt password hashing (work factor 10). Account locked for 15 minutes after 5 consecutive failed attempts.
- **Access Tokens:** Stateless HMAC-SHA256 JWTs with a 15-minute TTL. Claims include `userId`, `email`, `roles`, `institutionId`.
- **Refresh Tokens:** Opaque cryptographically random tokens stored in `refresh_tokens` table. Single-use rotation: every refresh issues a new token pair and revokes the predecessor. Revoked tokens are tracked to detect token theft.

---

## 5. Append-Only Audit Integrity

Audit events in `audit_events` cannot be edited or deleted by any application role or SQL user without disabling the database trigger:

```sql
CREATE OR REPLACE FUNCTION reject_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_events_no_update BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
```

---

## 6. Privacy & Minimal Disclosure Policy

In accordance with DPDP (Digital Personal Data Protection) norms:
- **Public Verification Endpoints (`/api/v1/verifications/public/{ref}`):** Exposes only masked identifiers (`900000****01`), student name (`Aarav S****`), institution name, and credential status.
- **QR Codes:** QR codes encode ONLY a signed reference URL (`https://apaar.gov.in/verify?ref=...`), NEVER raw Aadhaar numbers, APAAR IDs, or unencrypted student personally identifiable information (PII).

