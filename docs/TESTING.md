# Testing Strategy & Test Execution Guide

## 1. Multi-Tier Testing Strategy

The platform maintains a comprehensive testing pyramid across all layers:

1. **Unit Testing:**
   - **Backend:** JUnit 5, AssertJ, Mockito testing individual services, format validators, adapters, security claims, and data scopes.
   - **Frontend:** Vitest, React Testing Library testing UI components, form validations, format utilities, and role-based access rules.
   - **AI Service:** Pytest testing FastAPI routes, preprocessing transformations, ensemble scoring, and explainability contributions.
2. **Integration Testing:**
   - Backend integration tests with Testcontainers PostgreSQL verifying Flyway migrations, database constraints, and repository queries.
3. **End-to-End (E2E) Testing:**
   - Playwright verifying core multi-service user flows across browser UI and REST APIs.

---

## 2. Running Automated Tests

### 2.1 Backend Tests (JUnit 5 & Mockito)
```bash
# Run all backend unit tests
cd backend
mvn test

# Run a specific test class
mvn test -Dtest=ApaarFormatValidatorTest
```

### 2.2 Frontend Tests (Vitest & RTL)
```bash
# Run all frontend tests
cd frontend
npm test

# Run tests in watch mode
npm run test:watch
```

### 2.3 Python AI Service Tests (Pytest)
```bash
# Activate virtual environment and run pytest
cd ai-service
.venv/bin/pytest -v
```

---

## 3. Verified Test Suite Summary (Foundation Status)

| Test Suite | Framework | Total Tests | Passing | Failing | Execution Time |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Java Backend** | JUnit 5 + Mockito | 23 | 23 | 0 | 4.8s |
| **React Frontend** | Vitest + Testing Library | 14 | 14 | 0 | 1.7s |
| **Python AI Service** | Pytest + AnyIO | 56 | 56 | 0 | 1.9s |
| **Total Foundation** | | **93** | **93** | **0** | **~8.4s** |

