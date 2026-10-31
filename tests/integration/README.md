# Integration Test Suite

## Overview
Integration testing validates inter-service boundaries between:
- **Java Backend & PostgreSQL:** Flyway migrations, Hibernate JPA mappings, database triggers (`updated_at`, append-only `audit_events`), partial unique constraints.
- **Java Backend & FastAPI AI Service:** HTTP REST communication, feature vector serialization, anomaly score parsing, and timeout resilience.
- **Java Backend & APAAR Verification Adapter:** Deterministic response handling and fallback on gateway failure.

## Executing Integration Tests
```bash
# Backend Spring Boot integration tests with Testcontainers
cd backend
mvn verify -Dtest=*IntegrationTest
```

