# APAAR ID-Based Student Data Management and Verification Platform

A secure, scalable, demonstration-ready software platform developed for the Smart India Hackathon (SIH) problem statement:

**"Development of an APAAR ID-Based Student Data Management and Verification Platform for AICTE-Approved Institutions"**

The platform supports student-level data submission, APAAR identity resolution, admission compliance validation, AI-assisted anomaly detection, student record governance, academic verification, certificate integration, dashboards, and regulatory reporting.

---

## 1. Architecture Overview

```text
apaar-student-verification-platform/
├── frontend/               # React 18, TypeScript, Vite, Tailwind CSS, Recharts
├── backend/                # Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway
├── ai-service/             # Python 3.14, FastAPI, Pandas, Scikit-learn, Pydantic v2
├── database/               # PostgreSQL 16 migrations (V1–V5) and synthetic demo seed (V100)
├── integration/            # APAAR Verification adapter interface & mock documentation
├── deployment/             # Nginx reverse proxy, Prometheus, Grafana configs
├── tests/                  # Integration tests and E2E Playwright test foundation
├── docs/                   # Full architectural, database, API, and security documentation
├── compose.yaml            # Docker Compose orchestration
└── .env.example            # Environment template with local development defaults
```

---

## 2. Technology Stack

- **Frontend:** React 18, TypeScript 5.7, Vite 6, Tailwind CSS 3.4, Recharts 2.15, React Router 6.
- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Spring Batch, Flyway 10, JJWT 0.12, Apache POI 5.5, ZXing 3.5.
- **Database:** PostgreSQL 16 with version-controlled migrations and append-only audit trigger.
- **AI Service:** Python 3.14, FastAPI, Scikit-learn (IsolationForest ensemble), Pandas, NumPy, Pydantic v2.
- **Monitoring & Deployment:** Docker & Docker Compose, Actuator, Prometheus, Grafana, Nginx.

---

## 3. Quick Start (Local Development)

### Prerequisites
- Docker & Docker Compose **or**
- Local runtimes: OpenJDK 21+, Node.js 20+, Python 3.12+, PostgreSQL 16+.

### Option A: Running with Docker Compose (Recommended)

1. **Clone and prepare environment variables:**
   ```bash
   cp .env.example .env
   ```

2. **Start all services:**
   ```bash
   docker compose up --build -d
   ```

3. **Check health and logs:**
   ```bash
   docker compose ps
   docker compose logs -f backend
   ```

4. **Access the application:**
   - **Web UI:** [http://localhost:5173](http://localhost:5173)
   - **Backend REST API:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
   - **Swagger / OpenAPI Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **AI Service Health Check:** [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)
   - **Public Credential Verification:** [http://localhost:5173/verify](http://localhost:5173/verify)

---

### Option B: Running Bare Metal (Local Runtimes)

1. **Start PostgreSQL database:**
   Ensure PostgreSQL is running on port 5432 with database `apaar`, user `apaar`, and password `change-me-local-only`.

2. **Run Backend (Spring Boot):**
   ```bash
   cd backend
   mvn clean spring-boot:run
   ```

3. **Run AI Service (FastAPI):**
   ```bash
   cd ai-service
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements-dev.txt
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```

4. **Run Frontend (Vite Dev Server):**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

## 4. Synthetic Demo Accounts

All demo accounts share the local-only development password: **`Demo@12345`**

| Role | Email | Institution Scope | Purpose |
| :--- | :--- | :--- | :--- |
| **Platform Administrator** | `admin@apaar.local` | National / Global | Full system governance and user administration |
| **AICTE Regulatory Officer** | `officer@aicte.local` | National / Global | Cross-institution compliance, AI anomaly triage, case review |
| **Institution Administrator** | `admin@sunrise.demo` | Sunrise Institute (SIT) | Manage college programmes, quotas, and submissions |
| **Institution Data Operator** | `operator@sunrise.demo` | Sunrise Institute (SIT) | Upload student rosters and resolve row errors |
| **Case Reviewer** | `reviewer@apaar.local` | National / Global | Investigate cross-institution identity conflicts |
| **Verification Officer** | `verifier@sunrise.demo` | Sunrise Institute (SIT) | Issue academic verification tokens & QR references |
| **Independent Auditor** | `auditor@apaar.local` | National (Read-only) | Review immutable append-only audit trail |

---

## 5. Running Automated Tests

The platform includes **139 automated tests** verifying every layer:

```bash
# 1. Run Java Backend Tests (66 tests - JUnit 5 & Mockito)
cd backend && mvn test

# 2. Run React Frontend Tests (17 tests - Vitest & React Testing Library)
cd frontend && npm test

# 3. Run Python AI Service Tests (56 tests - Pytest)
cd ai-service && .venv/bin/pytest -v
```

---

## 6. Technical Documentation Index

Detailed architectural and design specifications are maintained in the [`docs/`](./docs/) directory:

- [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) — System boundaries, component topology, ADRs, and protocols.
- [`docs/DATABASE_DESIGN.md`](./docs/DATABASE_DESIGN.md) — Relational schema, ER diagram, partial indexes, and retention.
- [`docs/API_CONTRACTS.md`](./docs/API_CONTRACTS.md) — REST API catalog, request/response models, and error envelopes.
- [`docs/SECURITY_AND_RBAC.md`](./docs/SECURITY_AND_RBAC.md) — Security policy, 7 mandatory roles, and `AccessScopeService`.
- [`docs/WORKFLOWS.md`](./docs/WORKFLOWS.md) — End-to-end data lifecycle, state transitions, and audit logs.
- [`docs/INTEGRATIONS.md`](./docs/INTEGRATIONS.md) — APAAR Adapter interface, simulated adapter rules, and sandbox plan.
- [`docs/TESTING.md`](./docs/TESTING.md) — Test pyramid, execution commands, and verified status.
- [`docs/IMPLEMENTATION_PLAN.md`](./docs/IMPLEMENTATION_PLAN.md) — Ordered 8-stage roadmap across all 7 solution components.
