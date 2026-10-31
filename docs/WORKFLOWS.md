# End-to-End Data Lifecycles & State Transitions

## 1. Unified Student Data Integration Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Operator as Institution Operator
    participant UI as Frontend Portal
    participant API as Ingestion Service
    participant Validator as Schema & APAAR Validator
    participant DB as PostgreSQL DB
    participant Engine as Compliance & AI Engine

    Operator->>UI: Upload CSV / Excel Roster
    UI->>API: POST /api/v1/imports (Multipart)
    API->>DB: Create DataImportJob (STATUS: RECEIVED)
    API->>Validator: Validate Schema, Header Mapping, Constraints
    alt Format Errors Found
        Validator->>DB: Record Row Errors (DataImportErrors)
        Validator->>DB: Update Job (STATUS: COMPLETED_WITH_ERRORS)
        API-->>UI: Display Row-Level Error Diagnostics
        Operator->>UI: Fix rows & Reprocess
    else Validation Passes
        Validator->>DB: Batch Upsert Students & Enrolments (STATUS: IMPORTED)
        Validator->>DB: Update Job (STATUS: COMPLETED)
        API->>Engine: Trigger Compliance & Anomaly Analysis
    end
```

---

## 2. APAAR Identity Resolution & Conflict Workflow

```mermaid
stateDiagram-v2
    [*] --> FormSubmitted: Ingestion / Manual Entry
    FormSubmitted --> FormatInvalid: Regex/Checksum Fails
    FormSubmitted --> FormatValid: 12-Digit Numeric Match

    FormatInvalid --> CorrectionRequired: Return Error to Operator
    CorrectionRequired --> FormSubmitted: Resubmission

    FormatValid --> VerificationPending: Queue for Gateway Query
    VerificationPending --> Verified: Adapter Match (SIMULATED or AUTHORIZED)
    VerificationPending --> Mismatch: Attribute Collision (DOB/Name Mismatch)
    VerificationPending --> Unverified: Identity Not Found

    Mismatch --> IdentityConflictCreated: Flag Collision
    Unverified --> IdentityConflictCreated: Flag Missing Record

    IdentityConflictCreated --> Investigating: Assigned to Reviewer
    Investigating --> CaseDecisionRecorded: Evidence Collected & Action Decided
    CaseDecisionRecorded --> RecordCorrected: Student Record Version Incremented
    RecordCorrected --> [*]
    Verified --> [*]
```

---

## 3. Admission & Regulatory Compliance Workflow

1. **Intake Limits Check:** Evaluates whether total active enrolments exceed `approved_intake + supernumerary_intake`.
2. **Category Quota Check:** Checks category-specific counts (`GENERAL`, `OBC`, `SC`, `ST`, `EWS`) against sanctioned seats in `programme_intake_categories`.
3. **Cross-Institution Duplicate Enrolment Check:** Detects whether an APAAR ID or student identity is actively enrolled in two institutions during the same active academic year.
4. **Findings Generation:** Violations persist to `compliance_violations` with reason codes (`EXCESS_INTAKE`, `CATEGORY_EXCEEDED`, `CROSS_INSTITUTION_ACTIVE_ENROLMENT`).

---

## 4. AI-Assisted Anomaly Detection & Review Workflow

```mermaid
flowchart TD
    A[Student & Enrolment Dataset] --> B[AI Service: Feature Extraction]
    B --> C[IsolationForest & Rule Synergies]
    C --> D{Anomaly Score >= 0.65?}
    D -- No --> E[Mark Normal / Triaged]
    D -- Yes --> F[Flag Anomaly Result]
    F --> G[Generate Reason Codes & Feature Attribution]
    G --> H[Human Officer Triage Queue]
    H --> I{Officer Action}
    I -- False Positive --> J[Dismiss Anomaly with Notes]
    I -- Verified Discrepancy --> K[Open Formal Investigation Case]
```

---

## 5. Investigation Case State Lifecycle

- `OPEN`: Newly created case file triggered by compliance discrepancy, identity conflict, or AI alert.
- `ASSIGNED`: Case allocated to an AICTE Officer or Case Reviewer.
- `IN_REVIEW`: Case under active review; evidence attached and interview notes added.
- `PENDING_INFO`: Awaiting institutional clarification or documentary proof from college.
- `RESOLVED`: Formal decision rendered (`CORRECTION_APPROVED`, `DUPLICATE_CONFIRMED`, `ENROLMENT_CANCELLED`).
- `CLOSED`: Action implemented, audit record finalized.

---

## 6. Academic Verification & QR Lifecycle

1. **Issuance:** Verification Officer selects verified student enrolment and issues a `VerificationReference` with 180-day expiry and max use count (e.g. 10 uses).
2. **QR Generation:** Server generates a QR code containing a secure verification URL with `public_ref`.
3. **Public Lookup:** Employer scans QR code, triggering `GET /api/v1/verifications/public/{ref}`.
4. **Audit Logging:** Access event is recorded in `verification_events` with hashed client IP, timestamp, and status.

