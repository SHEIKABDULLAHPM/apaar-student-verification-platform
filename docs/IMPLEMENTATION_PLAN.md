# Phased Implementation Plan: All 7 Components

This document outlines the ordered, incremental roadmap for completing all seven solution components across the platform.

---

## Component Roadmap

### Stage 1: Project Foundation & Architecture (COMPLETED & VERIFIED)
- [x] Establish modular repository structure (`frontend/`, `backend/`, `ai-service/`, `database/`, `deployment/`, `docs/`).
- [x] Design complete PostgreSQL schema with Flyway migrations (`V1__initial_schema.sql`, `V2__reference_data.sql`, `V100__demo_seed.sql`).
- [x] Configure Spring Boot 3.5 backend with security, JWT, JPA, Actuator, Flyway, and OpenAPI.
- [x] Configure Python FastAPI AI service with preprocessing, detector, explainability, and 56 passing tests.
- [x] Configure React + Vite + Tailwind frontend with complete routing, auth context, layout shell, page views, and 14 passing tests.
- [x] Implement APAAR Adapter interface with deterministic simulated adapter.
- [x] Create Docker Compose and multi-stage Dockerfiles.
- [x] Verify 93 automated tests across backend, frontend, and AI service.

---

### Stage 2: Component 1 - Unified Student Data Integration
**Scope & Deliverables:**
1. Multi-format batch ingestion engine (CSV and Excel `.xlsx` via Apache POI & Commons CSV).
2. Spring Batch processing pipeline for asynchronous bulk ingestion of large datasets.
3. Row-level error logging, error download CSV, and UI re-submission modal.
4. Reconciliation engine matching new submissions against previous record versions.

**Dependencies:** Completed database tables (`data_import_jobs`, `data_import_rows`, `data_import_errors`), Spring Boot backend, and Frontend `ImportsPage`.

---

### Stage 3: Component 2 - APAAR Identity Resolution
**Scope & Deliverables:**
1. Background resolution queue evaluating batch APAAR identifiers.
2. Cross-institution duplicate detector identifying simultaneous active enrolments.
3. Automated creation of `identity_conflicts` with collision severity rating.
4. Institution conflict resolution workflow with evidence attachment.

**Dependencies:** Component 1 data ingestion, `ApaarVerificationPort`, and `IdentityConflictService`.

---

### Stage 4: Component 3 - Admission & Regulatory Compliance
**Scope & Deliverables:**
1. Configurable regulatory rule evaluator executing against baseline AICTE norms.
2. Category quota verification (General, OBC, SC, ST, EWS, Supernumerary, NRI, Foreign National).
3. Excess intake calculations and formal violation generation (`compliance_violations`).
4. Institutional self-compliance check before final roster submission.

**Dependencies:** Component 1 & 2 datasets, `admission_rules`, `compliance_evaluations`.

---

### Stage 5: Component 4 - AI-Assisted Regulatory Intelligence
**Scope & Deliverables:**
1. Orchestrate backend batch triggers calling FastAPI AI service `/anomalies/detect`.
2. Extract multi-dimensional feature vectors (enrolment dates, category ratios, late admissions).
3. Persist `anomaly_runs` and `anomaly_results` with explainable reason codes.
4. Human-in-the-loop triage interface for AICTE officers in Frontend `AnomaliesPage`.

**Dependencies:** FastAPI service endpoints, AI communication client in backend, Component 3 evaluation history.

---

### Stage 6: Component 5 - Trusted Student Registry & Case Management
**Scope & Deliverables:**
1. Student record versioning snapshots (`student_record_versions`) on any demographic modification.
2. Formal case docket management (`CASE-2026-XXXX`) linking conflicts, violations, and anomalies.
3. Evidentiary file upload with SHA-256 integrity checksums.
4. Formal case decision recording (`case_decisions`) with audit logs.

**Dependencies:** Components 2, 3, 4 cases and conflict records, `InvestigationCase` repository.

---

### Stage 7: Component 6 - Secure Academic Verification & Certificates
**Scope & Deliverables:**
1. Academic record publishing (semesters, SGPA, CGPA, credits).
2. QR code generation (via ZXing) embedding public verification tokens (`public_ref`).
3. Minimal-disclosure public verification endpoint (`/api/v1/verifications/public/{ref}`).
4. Tamper-evident certificate issuance with cryptographic checksums and revocation handling.

**Dependencies:** Student enrolment registry, verification reference repository, PublicVerifyPage.

---

### Stage 8: Component 7 - AICTE Dashboards & Regulatory Reporting
**Scope & Deliverables:**
1. Dynamic real-time calculation of national AICTE dashboard KPIs and institution dashboard metrics.
2. Filterable regulatory report generation and CSV export streaming (`/api/v1/reports/enrolments.csv`).
3. Comprehensive verification activity charts and intake vs enrolment heatmaps.
4. End-to-end integration verification and final demonstration readiness.

**Dependencies:** Data populated across all prior components.

