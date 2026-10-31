# APAAR AI Service

Standalone FastAPI service for **statistical anomaly detection and explainability** in the APAAR ID-Based Student Data Management and Verification Platform for AICTE-approved institutions.

> **Boundary statement**
> This service is investigation support only. It **never** performs authoritative APAAR ID verification and **never** makes deterministic regulatory, admission, eligibility, fraud, or enforcement decisions. Every flagged result is an investigation trigger that requires human review. It is trained and validated on **synthetic data only**.

The service is intentionally internal-only: no CORS middleware, no authentication layer, and no database. Deploy it behind the Java backend/API gateway on a private network.

---

## 1. What it does

- Deterministic rule signals for explicit enrolment/APAAR conditions.
- Robust z-scores against a fixed synthetic baseline.
- An IsolationForest ensemble component fitted on the same synthetic baseline.
- A bounded score (`0.0`–`1.0`), severity, reason codes, feature contributions, and human-readable explanations.
- Feature preview for debugging/explainability (no scoring).
- Offline synthetic evaluation metrics with an explicit non-real-world caveat.

## 2. What it does not do

- It does not verify APAAR IDs or student identity.
- It does not confirm fraud, non-compliance, or intent.
- It does not make authoritative or irreversible decisions.
- It does not use real student or institutional data.
- It is not a substitute for audit, institutional review, or official APAAR systems.

---

## 3. Project layout

```text
ai-service/
├── app/
│   ├── main.py                    # app factory, lifespan, middleware, handlers
│   ├── core/                      # settings, logging, constants
│   ├── api/routes/                # health, anomalies, features, evaluation
│   ├── schemas/                   # Pydantic v2 request/response contracts
│   └── services/                  # preprocessing, features, detector, explainability, evaluation
├── tests/                         # pytest suite (56 tests)
├── synthetic-data/
│   └── synthetic_students.csv     # small illustrative synthetic CSV
├── Dockerfile
├── requirements.txt
├── requirements-dev.txt
├── pyproject.toml
└── .env.example
```

---

## 4. Quick start

```bash
cd ai-service
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Swagger UI: <http://127.0.0.1:8000/docs>  
ReDoc: <http://127.0.0.1:8000/redoc>  
OpenAPI JSON: <http://127.0.0.1:8000/openapi.json>

`requirements.txt` contains runtime dependencies only. `requirements-dev.txt` adds `pytest` and `httpx` for tests.

### Configuration

Copy `.env.example` to `.env` if desired. Environment variables:

| Variable | Default | Meaning |
|---|---|---|
| `APP_ENV` | `local` | Environment label returned by `/health` |
| `MODEL_VERSION` | `0.1.0` | Version returned in every model response |
| `ANOMALY_THRESHOLD` | `0.55` | Score at/above which a record is flagged |
| `LOG_LEVEL` | `INFO` | `DEBUG`, `INFO`, `WARNING`, `ERROR`, `CRITICAL` |

No secrets are required.

---

## 5. API contract

### `GET /health`

```json
{
  "status": "ok",
  "service": "apaar-ai-service",
  "model_version": "0.1.0",
  "environment": "local"
}
```

### `GET /api/v1/model-info`

```json
{
  "model_name": "apaar-regulatory-anomaly",
  "model_version": "0.1.0",
  "methodology": "ensemble of robust z-score and isolation forest on synthetic baseline; investigation support only",
  "features": ["excess_intake_ratio", "..."],
  "limitations": ["..."],
  "threshold": 0.55,
  "trained_on_real_data": false
}
```

### `POST /api/v1/anomalies/score` and `/api/v1/anomalies/batch`

Both endpoints share the same request and response contract. A batch must contain **1–500** records; empty or oversize batches return `422`.

Request:

```json
{
  "records": [
    {
      "record_id": "uuid-or-string",
      "student_id": "uuid-or-null",
      "institution_id": "uuid",
      "programme_id": "uuid-or-null",
      "academic_year_id": "uuid-or-null",
      "admission_category": "GENERAL",
      "approved_intake": 120,
      "actual_enrolment": 130,
      "category_enrolment": 40,
      "category_capacity": 30,
      "apaar_format_valid": true,
      "apaar_verified": false,
      "duplicate_enrolment_count": 0,
      "credits_earned_ratio": 0.85,
      "cgpa": 7.4,
      "days_since_admission": 120,
      "institution_submission_delay_days": 3
    }
  ]
}
```

`admission_category` is one of `GENERAL`, `OBC`, `SC`, `ST`, `EWS`, `NRI`, `OCI`, `FOREIGN_NATIONAL`, `SUPERNUMERARY`.

Response:

```json
{
  "model_name": "apaar-regulatory-anomaly",
  "model_version": "0.1.0",
  "methodology": "ensemble of robust z-score and isolation forest on synthetic baseline; investigation support only",
  "trained_on_real_data": false,
  "results": [
    {
      "record_id": "uuid-or-string",
      "anomaly_score": 0.87,
      "is_anomaly": true,
      "severity": "HIGH",
      "reason_codes": ["EXCESS_INTAKE", "APAAR_UNVERIFIED"],
      "explanations": [
        {
          "code": "EXCESS_INTAKE",
          "message": "Reported enrolment (130) exceeds approved intake (120) by 10. Review the admission and approval records; this is an investigation trigger only.",
          "observed": 130,
          "expected": 120,
          "contribution": 0.42
        }
      ],
      "feature_contributions": {"excess_intake_ratio": 0.42},
      "requires_human_review": true
    }
  ],
  "summary": {"evaluated": 1, "flagged": 1}
}
```

### `POST /api/v1/features/preview`

Accepts the same `records` array, performs validation and feature engineering, and returns engineered columns/values **without scoring**:

```json
{
  "model_version": "0.1.0",
  "feature_columns": ["excess_intake_ratio", "..."],
  "rows": [
    {
      "record_id": "uuid-or-string",
      "features": {"excess_intake_ratio": 0.083333, "...": 0.0}
    }
  ],
  "count": 1,
  "scored": false,
  "note": "Feature preview only: ... No anomaly score, verification result, or regulatory decision is produced."
}
```

### `POST /api/v1/evaluate`

Accepts either a wrapper or a raw array of synthetic labelled feature rows:

```json
{
  "records": [
    {"features": {"excess_intake_ratio": 0.05, "cgpa": 7.2}, "label": 0},
    {"features": {"excess_intake_ratio": 1.10, "cgpa": 2.1}, "label": 1}
  ],
  "threshold": 0.55
}
```

The raw-array form is also accepted. Response contains `metrics` (precision, recall, F1, accuracy, support, positives, predicted_positive), a `confusion_matrix` object, and a note stating that synthetic evaluation **is not real-world validation**.

### Structured errors

All application errors use:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {"field": "records.0.cgpa", "message": "Input should be less than or equal to 10", "type": "less_than_equal"}
    ],
    "request_id": "..."
  }
}
```

Missing fields, negative values, out-of-range values, empty batches, oversize batches, duplicate `record_id`s, malformed JSON, and unknown admission categories return structured `422` errors. Request IDs are accepted via `X-Request-ID` (sanitised) or generated, and echoed in the response header and error body.

---

## 6. Detection methodology and determinism

Score construction:

1. **Rule signals** — explicit, explainable conditions such as excess intake, category capacity exceedance, invalid/unverified APAAR flags, duplicate signals, low credit progress, CGPA outside the plausible band, and delayed institution submission.
2. **Robust z-score** — directional median/MAD deviations against a fixed synthetic baseline (`n=600`, seed `20240517`).
3. **IsolationForest** — 200 estimators, `random_state=42`, fitted on the same synthetic baseline.

The weighted ensemble is clipped to `0.0`–`1.0`. Severity bands:

| Severity | Condition |
|---|---|
| `LOW` | below threshold / not flagged |
| `MEDIUM` | `score >= threshold` |
| `HIGH` | `score >= 0.70` |
| `CRITICAL` | `score >= 0.85` |

`requires_human_review` is `true` whenever a record is flagged. A record can carry a reason code while its aggregate score remains below threshold; reason codes are review context, not verdicts.

**Determinism:** the baseline, random seeds, weights and threshold are fixed. The same service build, model version and input produce the same score and explanations. Scores are not recomputed relative to other records in the batch.

### Reason codes

| Code | Trigger (investigation support) |
|---|---|
| `EXCESS_INTAKE` | `actual_enrolment > approved_intake` |
| `CATEGORY_CAPACITY_EXCEEDED` | `category_enrolment > category_capacity` |
| `APAAR_FORMAT_INVALID` | `apaar_format_valid = false` |
| `APAAR_UNVERIFIED` | `apaar_verified = false` |
| `DUPLICATE_ENROLMENT_SIGNAL` | `duplicate_enrolment_count > 0` |
| `LOW_CREDIT_PROGRESS` | `credits_earned_ratio < 0.60` |
| `CGPA_OUTLIER` | CGPA `< 4.0` or `> 9.8` |
| `SUBMISSION_DELAY` | submission delay `> 14` days |
| `STATISTICAL_OUTLIER` | max robust z-score `>= 3.5` |
| `MULTIVARIATE_OUTLIER` | IsolationForest component `>= 0.75` |

Messages never assert fraud or non-compliance.

---

## 7. Limitations

- Synthetic baseline only; no real-world training or validation.
- Alerts are investigation triggers, not proof of wrongdoing.
- No authoritative APAAR verification or deterministic regulatory findings.
- False positives and false negatives are expected; manual review is mandatory.
- Fixed baseline/threshold may not fit every institution, programme, or year.
- The evaluation endpoint is an offline synthetic tool; its in-sample IsolationForest metrics are not a validation study.
- The service is internal-only and has no auth/CORS/database by design.

See `/api/v1/model-info` for the same limitations in machine-readable form.

---

## 8. Privacy and logging

- Request bodies and raw record identifiers are not logged.
- Logs use a generated/sanitised request ID for correlation.
- Debug-level record references are SHA-256 hashes truncated to 10 characters; these are not a substitute for data-protection controls.
- No names, APAAR numbers, or student PII are required by this service.
- Use synthetic data only in development, tests and demos.

---

## 9. Tests

```bash
cd ai-service
python -m pytest
```

The suite covers health/model-info contracts, request IDs, docs, feature engineering/preview, normal and anomalous scoring, deterministic scores, every required reason code, explanation safety, validation failures (empty/oversize/negative/missing/duplicate), batch parity, structured errors, and synthetic evaluation metrics.

Latest local result: **56 passed**.

---

## 10. Docker

```bash
cd ai-service
docker build -t apaar-ai-service .
docker run --rm -p 8000:8000 -e APP_ENV=dev apaar-ai-service
```

The image uses `python:3.12-slim`, runs as the non-root `app` user, exposes port `8000`, and includes a `HEALTHCHECK` against `/health`.

---

## 11. Backend integration notes

- The Java backend should call `/api/v1/anomalies/score` (single or small batch) or `/api/v1/anomalies/batch`.
- Propagate `X-Request-ID` from the backend so logs can be correlated.
- Treat `is_anomaly`/`reason_codes` as review-queue inputs, not automated decisions.
- Display `severity`, `reason_codes`, `explanations` and `requires_human_review` to authorised reviewers.
- Never present model output as APAAR verification or a regulatory finding.
- The service ignores unknown request fields for forward compatibility; new required fields are a breaking change.
