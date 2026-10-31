-- ===========================================================================
-- Migration V5: Identity Resolution and Matching Engine
-- ===========================================================================

-- 1. Make student_id nullable in apaar_format_validations for standalone validation
ALTER TABLE apaar_format_validations ALTER COLUMN student_id DROP NOT NULL;

-- 2. Add fingerprint to identity_conflicts for duplicate suppression
ALTER TABLE identity_conflicts ADD COLUMN IF NOT EXISTS fingerprint VARCHAR(256);
CREATE UNIQUE INDEX IF NOT EXISTS identity_conflicts_fingerprint_uq 
    ON identity_conflicts (fingerprint) 
    WHERE status IN ('OPEN', 'INVESTIGATING');

-- 3. Extend case_decisions decision_code check constraint to support all standard identity resolution outcomes
ALTER TABLE case_decisions DROP CONSTRAINT IF EXISTS case_decisions_code_ck;
ALTER TABLE case_decisions ADD CONSTRAINT case_decisions_code_ck CHECK (decision_code IN (
    'NO_ACTION', 'CORRECTION_APPROVED', 'ENROLMENT_CANCELLED', 'REFERRED',
    'DUPLICATE_CONFIRMED', 'DATA_ERROR', 'IDENTITY_CONFIRMED', 'DUPLICATE_RECORD_CONFIRMED',
    'ENROLMENT_CONFLICT_CONFIRMED', 'DATA_ENTRY_ERROR', 'LEGITIMATE_HISTORICAL_ENROLMENT',
    'TRANSFER_OR_STATUS_CHANGE_CONFIRMED', 'NO_ACTION_REQUIRED', 'INSUFFICIENT_EVIDENCE',
    'REFERRED_FOR_FURTHER_REVIEW'
));

-- 4. Identity matching runs table
CREATE TABLE IF NOT EXISTS identity_matching_runs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_type             VARCHAR(48) NOT NULL DEFAULT 'EXACT_MATCH',
    status               VARCHAR(24) NOT NULL DEFAULT 'COMPLETED',
    records_scanned      INTEGER NOT NULL DEFAULT 0,
    matches_found        INTEGER NOT NULL DEFAULT 0,
    conflicts_generated  INTEGER NOT NULL DEFAULT 0,
    conflicts_suppressed INTEGER NOT NULL DEFAULT 0,
    run_by               UUID REFERENCES users(id) ON DELETE SET NULL,
    started_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    summary              JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX IF NOT EXISTS identity_matching_runs_status_idx ON identity_matching_runs (status, started_at DESC);

-- 5. Identity matches table
CREATE TABLE IF NOT EXISTS identity_matches (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id               UUID REFERENCES identity_matching_runs(id) ON DELETE SET NULL,
    match_type           VARCHAR(48) NOT NULL,
    matching_rule        VARCHAR(64) NOT NULL,
    primary_student_id   UUID REFERENCES students(id) ON DELETE RESTRICT,
    secondary_student_id UUID REFERENCES students(id) ON DELETE RESTRICT,
    primary_enrolment_id UUID REFERENCES enrolments(id) ON DELETE RESTRICT,
    secondary_enrolment_id UUID REFERENCES enrolments(id) ON DELETE RESTRICT,
    confidence_score     NUMERIC(5,4) NOT NULL DEFAULT 1.0000,
    match_classification VARCHAR(48) NOT NULL,
    review_required      BOOLEAN NOT NULL DEFAULT true,
    matching_fields      JSONB NOT NULL DEFAULT '[]'::jsonb,
    conflicting_fields   JSONB NOT NULL DEFAULT '[]'::jsonb,
    details              JSONB NOT NULL DEFAULT '{}'::jsonb,
    conflict_id          UUID REFERENCES identity_conflicts(id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS identity_matches_run_idx ON identity_matches (run_id);
CREATE INDEX IF NOT EXISTS identity_matches_primary_student_idx ON identity_matches (primary_student_id);
CREATE INDEX IF NOT EXISTS identity_matches_secondary_student_idx ON identity_matches (secondary_student_id);
CREATE INDEX IF NOT EXISTS identity_matches_conflict_idx ON identity_matches (conflict_id);
CREATE INDEX IF NOT EXISTS identity_matches_classification_idx ON identity_matches (match_classification);
