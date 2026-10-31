# REST API Specifications & Contracts

## 1. Global Conventions

- **Base URL:** `/api/v1`
- **Payload Format:** JSON (`application/json`)
- **Character Encoding:** UTF-8
- **Date/Time Format:** ISO 8601 UTC (`2026-10-30T14:20:00Z`)
- **Authentication:** HTTP Authorization Header `Bearer <jwt-token>`
- **Correlation ID:** Every request and response carries an `X-Request-Id` header.

### Standard Response Envelope
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

### Standard Error Response Format
```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Institution with id 11111111-1111-4111-8111-111111111111 does not exist.",
  "status": 404,
  "requestId": "req-98234-abcd",
  "timestamp": "2026-10-30T14:20:00Z",
  "fieldErrors": [
    {
      "field": "code",
      "message": "Institution code must not be blank"
    }
  ]
}
```

---

## 2. Resource Endpoints

### 2.1 Authentication & Profile (`/api/v1/auth`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Authenticate email/password; returns access & refresh tokens | Anonymous | Yes (login count) |
| `POST` | `/auth/refresh` | Exchange valid refresh token for new access token | Anonymous | Yes |
| `POST` | `/auth/logout` | Revoke active refresh token | Authenticated | Yes |
| `GET` | `/auth/me` | Fetch active user principal, assigned institution, and roles | Authenticated | No |

### 2.2 Institutions & Programmes (`/api/v1/institutions`, `/api/v1/programmes`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/institutions` | Paginated search of AICTE-approved institutions | `INSTITUTION_READ` | No |
| `GET` | `/institutions/{id}` | Retrieve specific institution profile | `INSTITUTION_READ` | No |
| `POST` | `/institutions` | Register new institution | `INSTITUTION_WRITE` | Yes |
| `GET` | `/programmes` | List programmes with sanctioned intake | `PROGRAMME_READ` | No |
| `POST` | `/programmes` | Add new academic programme and capacity | `PROGRAMME_WRITE` | Yes |
| `GET` | `/academic-years` | List academic years and active fiscal cycle | `ACADEMIC_YEAR_READ` | No |

### 2.3 Student Registry & Enrolments (`/api/v1/students`, `/api/v1/enrolments`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/students` | Search centralized student identity registry | `STUDENT_READ` | No |
| `GET` | `/students/{id}` | Get student detail with historical enrolment tree | `STUDENT_READ` | No |
| `POST` | `/students` | Register or update student identity | `STUDENT_WRITE` | Yes |
| `GET` | `/enrolments` | List institution-specific student enrolments | `ENROLMENT_READ` | No |
| `POST` | `/enrolments` | Submit single admission enrolment | `ENROLMENT_WRITE` | Yes |

### 2.4 Data Ingestion & Bulk Imports (`/api/v1/imports`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/imports` | Upload CSV/Excel roster (`multipart/form-data`) | `IMPORT_WRITE` | Yes |
| `GET` | `/imports` | List ingestion history and completion statuses | `IMPORT_READ` | No |
| `GET` | `/imports/{id}` | Inspect import job details and row counters | `IMPORT_READ` | No |
| `GET` | `/imports/{id}/errors` | Paginated row-level validation errors | `IMPORT_READ` | No |
| `POST` | `/imports/{id}/reprocess` | Re-run validation on corrected submission | `IMPORT_WRITE` | Yes |

### 2.5 APAAR Resolution & Verification (`/api/v1/apaar-verifications`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/apaar-verifications/format-validate` | Validate 12-digit structural syntax | `APAAR_VALIDATE` | Yes |
| `POST` | `/apaar-verifications/{studentId}/verify` | Execute gateway adapter query (simulated/authoritative) | `APAAR_VERIFY` | Yes |
| `GET` | `/identity-conflicts` | List cross-institution enrolment or APAAR collisions | `CONFLICT_READ` | No |
| `POST` | `/identity-conflicts/{id}/assign` | Assign conflict to officer for investigation | `CONFLICT_INVESTIGATE` | Yes |

### 2.6 Admission Compliance (`/api/v1/compliance`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/compliance/evaluate` | Trigger compliance audit for institution & year | `COMPLIANCE_RUN` | Yes |
| `GET` | `/compliance/evaluations` | List historical compliance audit runs | `COMPLIANCE_READ` | No |
| `GET` | `/compliance/violations` | Paginated list of flagged rule violations | `COMPLIANCE_READ` | No |

### 2.7 AI Regulatory Intelligence (`/api/v1/anomalies`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/anomalies/evaluate` | Dispatch batch inference job to Python AI service | `COMPLIANCE_RUN` | Yes |
| `GET` | `/anomalies` | Paginated list of AI anomaly scores & explanations | `ANOMALY_READ` | No |
| `POST` | `/anomalies/{id}/review` | Triage anomaly (Confirm, Dismiss, Open Case) | `ANOMALY_REVIEW` | Yes |

### 2.8 Investigation Cases (`/api/v1/cases`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/cases` | List open/investigating/resolved cases | `CASE_READ` | No |
| `POST` | `/cases` | Open new case file from conflict/violation/anomaly | `CASE_WRITE` | Yes |
| `GET` | `/cases/{id}` | Get full case dossier (links, evidence, comments) | `CASE_READ` | No |
| `POST` | `/cases/{id}/comments` | Add officer investigation note | `CASE_WRITE` | Yes |
| `POST` | `/cases/{id}/evidence` | Attach documentary proof | `CASE_WRITE` | Yes |
| `POST` | `/cases/{id}/decide` | Record official case resolution decision | `CASE_DECIDE` | Yes |

### 2.9 Academic Verification & Public Access (`/api/v1/verifications`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/verifications/issue` | Generate signed verification reference & QR token | `VERIFICATION_ISSUE` | Yes |
| `GET` | `/verifications/public/{ref}` | Public endpoint: Minimal disclosure verification | Anonymous / Public | Yes (event logged) |

### 2.10 Dashboards, Reports & Auditing (`/api/v1/dashboards`, `/reports`, `/audit`)
| Method | Endpoint | Description | Roles | Mutates |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/dashboards/aicte` | National regulator overview & KPI metrics | `DASHBOARD_READ` | No |
| `GET` | `/dashboards/institution` | Institution-scoped compliance overview | `DASHBOARD_READ` | No |
| `GET` | `/reports/enrolments.csv` | Stream server-calculated CSV report | `REPORT_EXPORT` | No |
| `GET` | `/audit` | Read immutable append-only audit trail | `AUDIT_READ` | No |

---

## 3. Python AI Service Internal Contract

- **Base URL:** `http://ai-service:8000/api/v1`
- **Endpoints:**
  - `GET /health`: Actuator health check returning model version and status.
  - `POST /anomalies/detect`: Batch inference endpoint accepting structured student enrolment records and returning ensemble anomaly scores, threshold comparisons, and Shapley-inspired reason codes.
  - `POST /features/extract`: Generates normalized numerical feature matrices from student records.
  - `POST /evaluation/metrics`: Returns model validation performance metrics (ROC-AUC, Precision, Recall).

