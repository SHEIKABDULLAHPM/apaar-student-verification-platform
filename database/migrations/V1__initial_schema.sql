-- ===========================================================================
-- APAAR Student Data Management and Verification Platform
-- Initial schema (PostgreSQL 16)
--
-- Design principles:
--   * Student identity is separate from institution-specific enrolments.
--   * Historical/repeated enrolments are legitimate; only active duplicates are
--     constrained at the database level.
--   * APAAR format validity is separate from authoritative verification.
--   * Evidence, evaluations and decisions are append/history oriented.
--   * All timestamps are stored as timestamptz (UTC) and rendered in IST by UI.
-- ===========================================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Institutions & academic configuration
-- ---------------------------------------------------------------------------
CREATE TABLE institutions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(32)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    aicte_approval_ref  VARCHAR(64),
    institution_type    VARCHAR(32)  NOT NULL DEFAULT 'AFFILIATED',
    address_line        VARCHAR(255),
    city                VARCHAR(128),
    district            VARCHAR(128),
    state               VARCHAR(128),
    pincode             VARCHAR(12),
    contact_email       VARCHAR(255),
    contact_phone       VARCHAR(32),
    status              VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    created_by          UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT institutions_code_uq UNIQUE (code),
    CONSTRAINT institutions_status_ck CHECK (status IN ('ACTIVE','SUSPENDED','INACTIVE')),
    CONSTRAINT institutions_type_ck CHECK (institution_type IN ('GOVERNMENT','PRIVATE','DEEMED','AFFILIATED','AUTONOMOUS'))
);
CREATE INDEX institutions_name_idx ON institutions (lower(name));
CREATE INDEX institutions_state_idx ON institutions (state);
CREATE TRIGGER institutions_updated_at BEFORE UPDATE ON institutions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE programmes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id  UUID NOT NULL REFERENCES institutions(id) ON DELETE RESTRICT,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    degree_level    VARCHAR(24)  NOT NULL,
    duration_years  NUMERIC(3,1) NOT NULL DEFAULT 4.0,
    status          VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    created_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT programmes_institution_code_uq UNIQUE (institution_id, code),
    CONSTRAINT programmes_level_ck CHECK (degree_level IN ('DIPLOMA','UG','PG','DOCTORAL')),
    CONSTRAINT programmes_status_ck CHECK (status IN ('ACTIVE','SUSPENDED','DISCONTINUED')),
    CONSTRAINT programmes_duration_ck CHECK (duration_years > 0)
);
CREATE INDEX programmes_institution_idx ON programmes (institution_id);
CREATE TRIGGER programmes_updated_at BEFORE UPDATE ON programmes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE academic_years (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(16) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT academic_years_code_uq UNIQUE (code),
    CONSTRAINT academic_years_dates_ck CHECK (end_date > start_date)
);
CREATE UNIQUE INDEX academic_years_one_active_idx ON academic_years (active) WHERE active;

CREATE TABLE programme_intakes (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    programme_id              UUID NOT NULL REFERENCES programmes(id) ON DELETE RESTRICT,
    academic_year_id          UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    approved_intake           INTEGER NOT NULL DEFAULT 0,
    supernumerary_intake      INTEGER NOT NULL DEFAULT 0,
    nri_intake                INTEGER NOT NULL DEFAULT 0,
    oci_intake                INTEGER NOT NULL DEFAULT 0,
    foreign_national_intake   INTEGER NOT NULL DEFAULT 0,
    status                    VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_by                UUID,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT programme_intakes_uq UNIQUE (programme_id, academic_year_id),
    CONSTRAINT programme_intakes_ck CHECK (
        approved_intake >= 0 AND supernumerary_intake >= 0 AND nri_intake >= 0
        AND oci_intake >= 0 AND foreign_national_intake >= 0
    ),
    CONSTRAINT programme_intakes_status_ck CHECK (status IN ('DRAFT','ACTIVE','CLOSED'))
);
CREATE INDEX programme_intakes_year_idx ON programme_intakes (academic_year_id);
CREATE TRIGGER programme_intakes_updated_at BEFORE UPDATE ON programme_intakes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE programme_intake_categories (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    programme_intake_id UUID NOT NULL REFERENCES programme_intakes(id) ON DELETE CASCADE,
    category           VARCHAR(32) NOT NULL,
    approved_seats     INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT programme_intake_categories_uq UNIQUE (programme_intake_id, category),
    CONSTRAINT programme_intake_categories_ck CHECK (approved_seats >= 0),
    CONSTRAINT programme_intake_categories_category_ck CHECK (category IN
        ('GENERAL','OBC','SC','ST','EWS','NRI','OCI','FOREIGN_NATIONAL','SUPERNUMERARY'))
);
CREATE TRIGGER programme_intake_categories_updated_at BEFORE UPDATE ON programme_intake_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Identity, roles and institution assignments
-- ---------------------------------------------------------------------------
CREATE TABLE roles (
    id          SMALLSERIAL PRIMARY KEY,
    code        VARCHAR(48) NOT NULL,
    name        VARCHAR(96) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT roles_code_uq UNIQUE (code)
);

CREATE TABLE permissions (
    code        VARCHAR(64) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE role_permissions (
    role_id         SMALLINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_code VARCHAR(64) NOT NULL REFERENCES permissions(code) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    institution_id        UUID REFERENCES institutions(id) ON DELETE RESTRICT,
    status                VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    password_changed_at   TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT users_email_lower_uq UNIQUE (email),
    CONSTRAINT users_status_ck CHECK (status IN ('ACTIVE','DISABLED','LOCKED'))
);
CREATE UNIQUE INDEX users_email_ci_uq ON users (lower(email));
CREATE INDEX users_institution_idx ON users (institution_id);
CREATE TRIGGER users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE institution_memberships (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    institution_id UUID NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    role_code      VARCHAR(48) NOT NULL REFERENCES roles(code) ON DELETE RESTRICT,
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT institution_memberships_uq UNIQUE (user_id, institution_id, role_code)
);
CREATE INDEX institution_memberships_institution_idx ON institution_memberships (institution_id);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT refresh_tokens_hash_uq UNIQUE (token_hash)
);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

-- ---------------------------------------------------------------------------
-- Students & record versions
-- ---------------------------------------------------------------------------
CREATE TABLE students (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apaar_id                   VARCHAR(12),
    apaar_format_valid         BOOLEAN NOT NULL DEFAULT false,
    apaar_format_validated_at  TIMESTAMPTZ,
    full_name                  VARCHAR(255) NOT NULL,
    date_of_birth              DATE,
    gender                     VARCHAR(24),
    category                   VARCHAR(32),
    email                      VARCHAR(255),
    phone                      VARCHAR(32),
    guardian_name              VARCHAR(255),
    address_line               VARCHAR(255),
    state                      VARCHAR(128),
    created_by                 UUID,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT students_gender_ck CHECK (gender IS NULL OR gender IN ('FEMALE','MALE','OTHER','PREFER_NOT_TO_SAY')),
    CONSTRAINT students_category_ck CHECK (category IS NULL OR category IN
        ('GENERAL','OBC','SC','ST','EWS','NRI','OCI','FOREIGN_NATIONAL','SUPERNUMERARY'))
);
CREATE UNIQUE INDEX students_apaar_id_uq ON students (apaar_id) WHERE apaar_id IS NOT NULL;
CREATE INDEX students_name_idx ON students (lower(full_name));
CREATE INDEX students_dob_idx ON students (date_of_birth);
CREATE TRIGGER students_updated_at BEFORE UPDATE ON students
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE student_record_versions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    version_no    INTEGER NOT NULL,
    snapshot      JSONB NOT NULL,
    change_reason VARCHAR(255) NOT NULL,
    changed_by    UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT student_record_versions_uq UNIQUE (student_id, version_no)
);
CREATE INDEX student_record_versions_student_idx ON student_record_versions (student_id, version_no DESC);

-- ---------------------------------------------------------------------------
-- Imports (created before enrolments so provenance FK is available)
-- ---------------------------------------------------------------------------
CREATE TABLE data_import_jobs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id    UUID NOT NULL REFERENCES institutions(id) ON DELETE RESTRICT,
    academic_year_id  UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    import_type       VARCHAR(32) NOT NULL DEFAULT 'ENROLMENTS',
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(128) NOT NULL,
    file_checksum     VARCHAR(64) NOT NULL,
    status              VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    total_rows          INTEGER NOT NULL DEFAULT 0,
    valid_rows          INTEGER NOT NULL DEFAULT 0,
    error_rows          INTEGER NOT NULL DEFAULT 0,
    idempotency_key     VARCHAR(128),
    uploaded_by         UUID REFERENCES users(id) ON DELETE RESTRICT,
    failure_reason      VARCHAR(500),
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT data_import_jobs_status_ck CHECK (status IN
        ('RECEIVED','VALIDATING','VALIDATED','PROCESSING','COMPLETED','COMPLETED_WITH_ERRORS','FAILED','CANCELLED')),
    CONSTRAINT data_import_jobs_type_ck CHECK (import_type IN ('ENROLMENTS','ACADEMIC_RECORDS','STUDENTS'))
);
CREATE UNIQUE INDEX data_import_jobs_idempotency_uq ON data_import_jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX data_import_jobs_institution_idx ON data_import_jobs (institution_id, created_at DESC);
CREATE INDEX data_import_jobs_status_idx ON data_import_jobs (status);
CREATE TRIGGER data_import_jobs_updated_at BEFORE UPDATE ON data_import_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE data_import_rows (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_job_id       UUID NOT NULL REFERENCES data_import_jobs(id) ON DELETE CASCADE,
    row_number          INTEGER NOT NULL,
    normalized_payload  JSONB,
    row_status          VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    matched_enrolment_id UUID,
    error_count         INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT data_import_rows_uq UNIQUE (import_job_id, row_number),
    CONSTRAINT data_import_rows_status_ck CHECK (row_status IN
        ('PENDING','VALID','INVALID','IMPORTED','RECONCILED','SKIPPED'))
);
CREATE INDEX data_import_rows_job_idx ON data_import_rows (import_job_id, row_number);

CREATE TABLE data_import_errors (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_job_id UUID NOT NULL REFERENCES data_import_jobs(id) ON DELETE CASCADE,
    row_number    INTEGER NOT NULL,
    column_name   VARCHAR(64),
    error_code    VARCHAR(64) NOT NULL,
    message       VARCHAR(500) NOT NULL,
    raw_value     VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX data_import_errors_job_idx ON data_import_errors (import_job_id, row_number);

-- ---------------------------------------------------------------------------
-- Enrolments (institution-specific; history retained)
-- ---------------------------------------------------------------------------
CREATE TABLE enrolments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    institution_id    UUID NOT NULL REFERENCES institutions(id) ON DELETE RESTRICT,
    programme_id      UUID NOT NULL REFERENCES programmes(id) ON DELETE RESTRICT,
    academic_year_id  UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    enrolment_number  VARCHAR(64) NOT NULL,
    admission_category VARCHAR(32) NOT NULL,
    quota_type        VARCHAR(32),
    admission_date    DATE,
    status            VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    source_import_id  UUID REFERENCES data_import_jobs(id) ON DELETE SET NULL,
    created_by        UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT enrolments_number_uq UNIQUE (institution_id, enrolment_number),
    CONSTRAINT enrolments_category_ck CHECK (admission_category IN
        ('GENERAL','OBC','SC','ST','EWS','NRI','OCI','FOREIGN_NATIONAL','SUPERNUMERARY')),
    CONSTRAINT enrolments_status_ck CHECK (status IN
        ('PROVISIONAL','ACTIVE','COMPLETED','WITHDRAWN','CANCELLED','SUSPENDED'))
);
CREATE UNIQUE INDEX enrolments_active_student_programme_year_uq
    ON enrolments (student_id, programme_id, academic_year_id)
    WHERE status IN ('PROVISIONAL','ACTIVE');
CREATE INDEX enrolments_institution_year_idx ON enrolments (institution_id, academic_year_id);
CREATE INDEX enrolments_programme_year_idx ON enrolments (programme_id, academic_year_id);
CREATE INDEX enrolments_student_idx ON enrolments (student_id);
CREATE INDEX enrolments_import_idx ON enrolments (source_import_id);
CREATE TRIGGER enrolments_updated_at BEFORE UPDATE ON enrolments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- APAAR format validation vs authoritative/simulated verification
-- ---------------------------------------------------------------------------
CREATE TABLE apaar_format_validations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    apaar_id     VARCHAR(12) NOT NULL,
    is_valid     BOOLEAN NOT NULL,
    reason_code  VARCHAR(64) NOT NULL,
    validated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    validated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX apaar_format_validations_student_idx ON apaar_format_validations (student_id, validated_at DESC);

CREATE TABLE apaar_verification_records (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    apaar_id_submitted VARCHAR(12) NOT NULL,
    verification_mode  VARCHAR(24) NOT NULL,
    adapter_name       VARCHAR(96) NOT NULL,
    status             VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128),
    reason_code        VARCHAR(64),
    response_payload   JSONB,
    requested_by       UUID REFERENCES users(id) ON DELETE RESTRICT,
    requested_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified_at        TIMESTAMPTZ,
    expires_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT apaar_verification_mode_ck CHECK (verification_mode IN ('SIMULATED','AUTHORIZED')),
    CONSTRAINT apaar_verification_status_ck CHECK (status IN
        ('PENDING','VERIFIED','MISMATCH','FAILED','NOT_VERIFIED','EXPIRED'))
);
CREATE INDEX apaar_verification_student_idx ON apaar_verification_records (student_id, requested_at DESC);
CREATE INDEX apaar_verification_status_idx ON apaar_verification_records (status);
CREATE INDEX apaar_verification_apaar_idx ON apaar_verification_records (apaar_id_submitted);

-- ---------------------------------------------------------------------------
-- Configurable admission rules, evaluations and findings
-- ---------------------------------------------------------------------------
CREATE TABLE admission_rules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(64) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    rule_type        VARCHAR(48) NOT NULL,
    category         VARCHAR(32) NOT NULL DEFAULT 'ADMISSION',
    config           JSONB NOT NULL DEFAULT '{}'::jsonb,
    severity         VARCHAR(16) NOT NULL DEFAULT 'HIGH',
    active           BOOLEAN NOT NULL DEFAULT true,
    version          INTEGER NOT NULL DEFAULT 1,
    effective_from   DATE,
    effective_to     DATE,
    source_reference VARCHAR(255) NOT NULL DEFAULT 'CONFIGURABLE_BASELINE_NOT_OFFICIAL',
    created_by       UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT admission_rules_code_uq UNIQUE (code),
    CONSTRAINT admission_rules_severity_ck CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT admission_rules_type_ck CHECK (rule_type IN
        ('INTAKE_LIMIT','CATEGORY_CAPACITY','DUPLICATE_ENROLMENT','APAAR_FORMAT','APAAR_VERIFICATION','DATA_COMPLETENESS'))
);
CREATE INDEX admission_rules_active_idx ON admission_rules (active);
CREATE TRIGGER admission_rules_updated_at BEFORE UPDATE ON admission_rules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE compliance_evaluations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id   UUID NOT NULL REFERENCES institutions(id) ON DELETE RESTRICT,
    programme_id     UUID REFERENCES programmes(id) ON DELETE RESTRICT,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    ruleset_version  INTEGER NOT NULL DEFAULT 1,
    evaluated_by     UUID REFERENCES users(id) ON DELETE RESTRICT,
    evaluated_at     TIMESTAMPTZ,
    total_findings   INTEGER NOT NULL DEFAULT 0,
    summary          JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT compliance_evaluations_status_ck CHECK (status IN
        ('PENDING','RUNNING','COMPLETED','FAILED'))
);
CREATE INDEX compliance_evaluations_scope_idx ON compliance_evaluations
    (institution_id, academic_year_id, created_at DESC);

CREATE TABLE compliance_violations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id UUID NOT NULL REFERENCES compliance_evaluations(id) ON DELETE RESTRICT,
    rule_id       UUID REFERENCES admission_rules(id) ON DELETE RESTRICT,
    enrolment_id  UUID REFERENCES enrolments(id) ON DELETE RESTRICT,
    student_id    UUID REFERENCES students(id) ON DELETE RESTRICT,
    reason_code   VARCHAR(64) NOT NULL,
    severity      VARCHAR(16) NOT NULL,
    details       JSONB NOT NULL DEFAULT '{}'::jsonb,
    status        VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    resolved_by   UUID REFERENCES users(id) ON DELETE RESTRICT,
    resolved_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT compliance_violations_severity_ck CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT compliance_violations_status_ck CHECK (status IN
        ('OPEN','ACKNOWLEDGED','RESOLVED','WAIVED'))
);
CREATE INDEX compliance_violations_eval_idx ON compliance_violations (evaluation_id);
CREATE INDEX compliance_violations_status_idx ON compliance_violations (status);
CREATE INDEX compliance_violations_enrolment_idx ON compliance_violations (enrolment_id);

-- ---------------------------------------------------------------------------
-- Identity conflicts
-- ---------------------------------------------------------------------------
CREATE TABLE identity_conflicts (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conflict_type         VARCHAR(48) NOT NULL,
    primary_student_id    UUID REFERENCES students(id) ON DELETE RESTRICT,
    secondary_student_id  UUID REFERENCES students(id) ON DELETE RESTRICT,
    primary_enrolment_id  UUID REFERENCES enrolments(id) ON DELETE RESTRICT,
    secondary_enrolment_id UUID REFERENCES enrolments(id) ON DELETE RESTRICT,
    detection_source      VARCHAR(24) NOT NULL DEFAULT 'RULE',
    status                VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    severity              VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    reason_code           VARCHAR(64) NOT NULL,
    details               JSONB NOT NULL DEFAULT '{}'::jsonb,
    assigned_to           UUID REFERENCES users(id) ON DELETE RESTRICT,
    resolved_by           UUID REFERENCES users(id) ON DELETE RESTRICT,
    resolution_notes      VARCHAR(2000),
    resolved_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT identity_conflicts_source_ck CHECK (detection_source IN ('RULE','AI','MANUAL','IMPORT')),
    CONSTRAINT identity_conflicts_status_ck CHECK (status IN
        ('OPEN','INVESTIGATING','RESOLVED','DISMISSED')),
    CONSTRAINT identity_conflicts_severity_ck CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT identity_conflicts_pair_ck CHECK (
        primary_student_id IS NOT NULL OR secondary_student_id IS NOT NULL
        OR primary_enrolment_id IS NOT NULL OR secondary_enrolment_id IS NOT NULL
    )
);
CREATE INDEX identity_conflicts_status_idx ON identity_conflicts (status, created_at DESC);
CREATE INDEX identity_conflicts_primary_student_idx ON identity_conflicts (primary_student_id);
CREATE INDEX identity_conflicts_secondary_student_idx ON identity_conflicts (secondary_student_id);
CREATE TRIGGER identity_conflicts_updated_at BEFORE UPDATE ON identity_conflicts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- AI anomaly runs & results
-- ---------------------------------------------------------------------------
CREATE TABLE anomaly_runs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id   UUID REFERENCES institutions(id) ON DELETE RESTRICT,
    academic_year_id UUID REFERENCES academic_years(id) ON DELETE RESTRICT,
    model_name       VARCHAR(96) NOT NULL,
    model_version    VARCHAR(32) NOT NULL,
    requested_by     UUID REFERENCES users(id) ON DELETE RESTRICT,
    status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    records_evaluated INTEGER NOT NULL DEFAULT 0,
    flagged_count    INTEGER NOT NULL DEFAULT 0,
    error_message    VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ,
    CONSTRAINT anomaly_runs_status_ck CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);
CREATE INDEX anomaly_runs_scope_idx ON anomaly_runs (institution_id, created_at DESC);

CREATE TABLE anomaly_results (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id         UUID REFERENCES anomaly_runs(id) ON DELETE SET NULL,
    entity_type    VARCHAR(32) NOT NULL,
    entity_id      UUID NOT NULL,
    student_id     UUID REFERENCES students(id) ON DELETE RESTRICT,
    enrolment_id   UUID REFERENCES enrolments(id) ON DELETE RESTRICT,
    model_name     VARCHAR(96) NOT NULL,
    model_version  VARCHAR(32) NOT NULL,
    anomaly_score  NUMERIC(6,4) NOT NULL,
    threshold      NUMERIC(6,4) NOT NULL,
    is_anomaly     BOOLEAN NOT NULL,
    severity       VARCHAR(16) NOT NULL,
    reason_codes   TEXT[] NOT NULL DEFAULT '{}',
    explanations   JSONB NOT NULL DEFAULT '[]'::jsonb,
    features       JSONB NOT NULL DEFAULT '{}'::jsonb,
    status         VARCHAR(24) NOT NULL DEFAULT 'NEW',
    reviewed_by    UUID REFERENCES users(id) ON DELETE RESTRICT,
    reviewed_at    TIMESTAMPTZ,
    review_notes   VARCHAR(2000),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT anomaly_results_score_ck CHECK (anomaly_score >= 0 AND anomaly_score <= 1),
    CONSTRAINT anomaly_results_threshold_ck CHECK (threshold >= 0 AND threshold <= 1),
    CONSTRAINT anomaly_results_severity_ck CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT anomaly_results_status_ck CHECK (status IN
        ('NEW','TRIAGED','INVESTIGATING','CONFIRMED','DISMISSED'))
);
CREATE INDEX anomaly_results_status_idx ON anomaly_results (status, anomaly_score DESC);
CREATE INDEX anomaly_results_enrolment_idx ON anomaly_results (enrolment_id);
CREATE INDEX anomaly_results_entity_idx ON anomaly_results (entity_type, entity_id);
CREATE INDEX anomaly_results_run_idx ON anomaly_results (run_id);

-- ---------------------------------------------------------------------------
-- Investigation cases
-- ---------------------------------------------------------------------------
CREATE TABLE investigation_cases (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number        VARCHAR(32) NOT NULL,
    institution_id     UUID REFERENCES institutions(id) ON DELETE RESTRICT,
    case_type          VARCHAR(48) NOT NULL,
    title              VARCHAR(255) NOT NULL,
    description        VARCHAR(2000),
    priority           VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status             VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    assigned_to        UUID REFERENCES users(id) ON DELETE RESTRICT,
    opened_by          UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    opened_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at             TIMESTAMPTZ,
    closed_at          TIMESTAMPTZ,
    resolution_summary VARCHAR(2000),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT investigation_cases_number_uq UNIQUE (case_number),
    CONSTRAINT investigation_cases_status_ck CHECK (status IN
        ('OPEN','ASSIGNED','IN_REVIEW','PENDING_INFO','RESOLVED','CLOSED')),
    CONSTRAINT investigation_cases_priority_ck CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT investigation_cases_type_ck CHECK (case_type IN
        ('IDENTITY_CONFLICT','COMPLIANCE','ANOMALY','DATA_QUALITY','VERIFICATION','OTHER'))
);
CREATE INDEX investigation_cases_status_idx ON investigation_cases (status, created_at DESC);
CREATE INDEX investigation_cases_institution_idx ON investigation_cases (institution_id);
CREATE INDEX investigation_cases_assignee_idx ON investigation_cases (assigned_to);
CREATE TRIGGER investigation_cases_updated_at BEFORE UPDATE ON investigation_cases
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE case_links (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id     UUID NOT NULL REFERENCES investigation_cases(id) ON DELETE CASCADE,
    entity_type VARCHAR(48) NOT NULL,
    entity_id   UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT case_links_uq UNIQUE (case_id, entity_type, entity_id)
);
CREATE INDEX case_links_entity_idx ON case_links (entity_type, entity_id);

CREATE TABLE case_evidence (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id         UUID NOT NULL REFERENCES investigation_cases(id) ON DELETE RESTRICT,
    evidence_type   VARCHAR(48) NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    storage_ref     VARCHAR(500),
    checksum_sha256 VARCHAR(64),
    collected_by    UUID REFERENCES users(id) ON DELETE RESTRICT,
    collected_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX case_evidence_case_idx ON case_evidence (case_id);

CREATE TABLE case_comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id    UUID NOT NULL REFERENCES investigation_cases(id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    body       VARCHAR(4000) NOT NULL,
    internal   BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX case_comments_case_idx ON case_comments (case_id, created_at);

CREATE TABLE case_decisions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id          UUID NOT NULL REFERENCES investigation_cases(id) ON DELETE RESTRICT,
    decision_code    VARCHAR(48) NOT NULL,
    rationale        VARCHAR(4000) NOT NULL,
    effective_action VARCHAR(500),
    decided_by       UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decided_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT case_decisions_code_ck CHECK (decision_code IN
        ('NO_ACTION','CORRECTION_APPROVED','ENROLMENT_CANCELLED','REFERRED','DUPLICATE_CONFIRMED','DATA_ERROR'))
);
CREATE INDEX case_decisions_case_idx ON case_decisions (case_id, decided_at DESC);

-- ---------------------------------------------------------------------------
-- Academic records, verification references/events, certificates
-- ---------------------------------------------------------------------------
CREATE TABLE academic_records (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrolment_id     UUID NOT NULL REFERENCES enrolments(id) ON DELETE RESTRICT,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    semester         INTEGER NOT NULL,
    credits_earned   NUMERIC(6,2) NOT NULL DEFAULT 0,
    credits_total    NUMERIC(6,2) NOT NULL DEFAULT 0,
    sgpa             NUMERIC(4,2),
    cgpa             NUMERIC(4,2),
    result_status    VARCHAR(24) NOT NULL DEFAULT 'PASS',
    published_at     TIMESTAMPTZ,
    source           VARCHAR(48) NOT NULL DEFAULT 'INSTITUTION_SUBMISSION',
    created_by       UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT academic_records_uq UNIQUE (enrolment_id, academic_year_id, semester),
    CONSTRAINT academic_records_semester_ck CHECK (semester BETWEEN 1 AND 16),
    CONSTRAINT academic_records_credits_ck CHECK (credits_earned >= 0 AND credits_total >= 0 AND credits_earned <= credits_total),
    CONSTRAINT academic_records_result_ck CHECK (result_status IN ('PASS','FAIL','WITHHELD','PENDING')),
    CONSTRAINT academic_records_sgpa_ck CHECK (sgpa IS NULL OR (sgpa >= 0 AND sgpa <= 10)),
    CONSTRAINT academic_records_cgpa_ck CHECK (cgpa IS NULL OR (cgpa >= 0 AND cgpa <= 10))
);
CREATE INDEX academic_records_enrolment_idx ON academic_records (enrolment_id, semester);
CREATE TRIGGER academic_records_updated_at BEFORE UPDATE ON academic_records
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE verification_references (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_ref         VARCHAR(64) NOT NULL,
    academic_record_id UUID REFERENCES academic_records(id) ON DELETE RESTRICT,
    enrolment_id       UUID NOT NULL REFERENCES enrolments(id) ON DELETE RESTRICT,
    purpose            VARCHAR(255) NOT NULL,
    status             VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    issued_by          UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    issued_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at         TIMESTAMPTZ NOT NULL,
    max_uses           INTEGER NOT NULL DEFAULT 10,
    use_count          INTEGER NOT NULL DEFAULT 0,
    revoked_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT verification_references_ref_uq UNIQUE (public_ref),
    CONSTRAINT verification_references_status_ck CHECK (status IN ('ACTIVE','EXPIRED','REVOKED','EXHAUSTED')),
    CONSTRAINT verification_references_uses_ck CHECK (max_uses > 0 AND use_count >= 0 AND use_count <= max_uses)
);
CREATE INDEX verification_references_enrolment_idx ON verification_references (enrolment_id);

CREATE TABLE verification_events (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_reference_id UUID NOT NULL REFERENCES verification_references(id) ON DELETE RESTRICT,
    event_type               VARCHAR(32) NOT NULL,
    actor_type               VARCHAR(24) NOT NULL,
    actor_id                 VARCHAR(128),
    ip_hash                  VARCHAR(64),
    user_agent               VARCHAR(500),
    result                   VARCHAR(32) NOT NULL,
    details                  JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT verification_events_actor_ck CHECK (actor_type IN ('PUBLIC','INSTITUTION','AICTE','SYSTEM')),
    CONSTRAINT verification_events_result_ck CHECK (result IN ('SUCCESS','DENIED','EXPIRED','REVOKED','ERROR'))
);
CREATE INDEX verification_events_ref_idx ON verification_events (verification_reference_id, occurred_at DESC);

CREATE TABLE certificates (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrolment_id             UUID NOT NULL REFERENCES enrolments(id) ON DELETE RESTRICT,
    certificate_type         VARCHAR(64) NOT NULL,
    certificate_number       VARCHAR(96) NOT NULL,
    issued_by_institution_id UUID NOT NULL REFERENCES institutions(id) ON DELETE RESTRICT,
    issued_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    status                   VARCHAR(24) NOT NULL DEFAULT 'ISSUED',
    document_checksum        VARCHAR(64),
    metadata                 JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT certificates_number_uq UNIQUE (certificate_number),
    CONSTRAINT certificates_status_ck CHECK (status IN ('ISSUED','REVOKED','SUPERSEDED'))
);
CREATE INDEX certificates_enrolment_idx ON certificates (enrolment_id);

-- ---------------------------------------------------------------------------
-- Audit, notifications, retention and idempotency
-- ---------------------------------------------------------------------------
CREATE TABLE audit_events (
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(64) NOT NULL,
    actor_id       UUID,
    actor_role     VARCHAR(64),
    institution_id UUID,
    entity_type    VARCHAR(64) NOT NULL,
    entity_id      VARCHAR(64),
    action         VARCHAR(48) NOT NULL,
    outcome        VARCHAR(24) NOT NULL DEFAULT 'SUCCESS',
    request_id     VARCHAR(64),
    ip_hash        VARCHAR(64),
    metadata       JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT audit_events_outcome_ck CHECK (outcome IN ('SUCCESS','DENIED','FAILURE'))
);
CREATE INDEX audit_events_occurred_idx ON audit_events (occurred_at DESC);
CREATE INDEX audit_events_actor_idx ON audit_events (actor_id, occurred_at DESC);
CREATE INDEX audit_events_entity_idx ON audit_events (entity_type, entity_id);
CREATE INDEX audit_events_institution_idx ON audit_events (institution_id, occurred_at DESC);

-- Audit events are append-only: corrections are recorded as new events.
CREATE OR REPLACE FUNCTION reject_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER audit_events_no_update BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

CREATE TABLE notifications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    institution_id    UUID REFERENCES institutions(id) ON DELETE CASCADE,
    type              VARCHAR(48) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    body              VARCHAR(2000) NOT NULL,
    severity          VARCHAR(16) NOT NULL DEFAULT 'INFO',
    read_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT notifications_severity_ck CHECK (severity IN ('INFO','WARNING','CRITICAL'))
);
CREATE INDEX notifications_recipient_idx ON notifications (recipient_user_id, created_at DESC);

CREATE TABLE retention_policies (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type       VARCHAR(64) NOT NULL,
    retention_years   INTEGER NOT NULL,
    action            VARCHAR(32) NOT NULL,
    legal_basis_note  VARCHAR(1000) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT retention_policies_entity_uq UNIQUE (entity_type),
    CONSTRAINT retention_policies_years_ck CHECK (retention_years > 0),
    CONSTRAINT retention_policies_action_ck CHECK (action IN ('ANONYMIZE','ARCHIVE','RETAIN','DELETE'))
);

CREATE TABLE idempotency_keys (
    key             VARCHAR(128) PRIMARY KEY,
    user_id         UUID REFERENCES users(id) ON DELETE CASCADE,
    endpoint        VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idempotency_keys_expiry_idx ON idempotency_keys (expires_at);
