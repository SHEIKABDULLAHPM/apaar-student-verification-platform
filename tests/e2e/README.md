# End-to-End Testing Suite (Playwright)

## Overview
This directory contains end-to-end test specifications verifying cross-service user journeys across the APAAR Student Verification Platform:
1. Institution Administrator login and student CSV data ingestion.
2. Schema validation, error reporting, and row-level correction.
3. APAAR Identity Resolution & Simulated Verification.
4. AICTE Officer regulatory dashboard review and intake vs enrolment compliance checking.
5. AI anomaly alert review, investigation case creation, and evidence tracking.
6. Public verification lookup using reference QR token.
7. Role-based access control and cross-institution data isolation enforcement.

## Running Tests

### Prerequisites
- Node.js 22+
- Platform running via Docker Compose (`docker compose up -d`) or local processes.

```bash
# Install Playwright dependencies
npm install -D @playwright/test

# Run E2E tests
npx playwright test
```

