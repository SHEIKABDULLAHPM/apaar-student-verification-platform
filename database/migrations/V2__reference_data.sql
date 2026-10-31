-- ===========================================================================
-- Reference data: roles, permissions, role-permission matrix, configurable
-- baseline rules and retention policy defaults.
--
-- IMPORTANT: admission rules below are CONFIGURABLE BASELINE RULES for the
-- prototype. They are not official AICTE regulations and carry the source
-- marker "CONFIGURABLE_BASELINE_NOT_OFFICIAL". Real rules must be loaded from
-- a verified regulatory source through the rule configuration workflow.
-- ===========================================================================

INSERT INTO roles (code, name, description) VALUES
    ('PLATFORM_ADMIN',        'Platform Administrator', 'Operates the platform, users, configuration and all regulatory data.'),
    ('AICTE_OFFICER',         'AICTE Regulatory Officer', 'Reviews national compliance, anomalies, cases and reports.'),
    ('INSTITUTION_ADMIN',     'Institution Administrator', 'Manages an approved institution profile, programmes, data and staff.'),
    ('INSTITUTION_OPERATOR',  'Institution Data Operator', 'Submits student and enrolment data for one institution.'),
    ('VERIFICATION_OFFICER',  'Verification Officer', 'Issues and controls academic verification references.'),
    ('CASE_REVIEWER',         'Case Reviewer', 'Investigates identity conflicts, compliance findings and AI alerts.'),
    ('AUDITOR',               'Auditor', 'Read-only access to records, decisions and audit evidence.');

INSERT INTO permissions (code, description) VALUES
    ('INSTITUTION_READ',      'Read institution profiles'),
    ('INSTITUTION_WRITE',     'Create or update institution profiles'),
    ('PROGRAMME_READ',        'Read programmes and approved intake'),
    ('PROGRAMME_WRITE',       'Create or update programmes and intake'),
    ('ACADEMIC_YEAR_READ',    'Read academic years'),
    ('ACADEMIC_YEAR_WRITE',   'Manage academic years'),
    ('STUDENT_READ',          'Read student identity records'),
    ('STUDENT_WRITE',         'Create or correct student records'),
    ('ENROLMENT_READ',        'Read institution-specific enrolments'),
    ('ENROLMENT_WRITE',       'Create or correct enrolments'),
    ('IMPORT_READ',           'Read import jobs and row errors'),
    ('IMPORT_WRITE',          'Upload and reprocess imports'),
    ('APAAR_VALIDATE',        'Run APAAR format validation'),
    ('APAAR_VERIFY',          'Request APAAR verification through an adapter'),
    ('CONFLICT_READ',         'Read identity conflicts'),
    ('CONFLICT_INVESTIGATE',  'Progress identity conflict investigations'),
    ('COMPLIANCE_READ',       'Read compliance evaluations and findings'),
    ('COMPLIANCE_RUN',        'Run compliance evaluations'),
    ('RULE_WRITE',            'Configure admission rules'),
    ('ANOMALY_READ',          'Read AI-assisted anomaly results'),
    ('ANOMALY_REVIEW',        'Triage or review AI-assisted alerts'),
    ('CASE_READ',             'Read investigation cases'),
    ('CASE_WRITE',            'Open cases, add evidence and comments'),
    ('CASE_DECIDE',           'Record case decisions and corrections'),
    ('VERIFICATION_ISSUE',    'Issue academic verification references and QR codes'),
    ('VERIFICATION_READ',     'Read verification history'),
    ('CERTIFICATE_READ',      'Read certificate verification metadata'),
    ('DASHBOARD_READ',        'Read role-scoped dashboards'),
    ('REPORT_EXPORT',         'Export regulatory reports'),
    ('AUDIT_READ',            'Read append-only audit events'),
    ('USER_ADMIN',            'Manage users and role assignments'),
    ('RETENTION_ADMIN',       'Manage retention policies');

-- Platform administrator receives every permission.
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code FROM roles r CROSS JOIN permissions p WHERE r.code = 'PLATFORM_ADMIN';

INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, x.permission_code FROM roles r
JOIN (VALUES
    ('AICTE_OFFICER','INSTITUTION_READ'), ('AICTE_OFFICER','PROGRAMME_READ'),
    ('AICTE_OFFICER','ACADEMIC_YEAR_READ'), ('AICTE_OFFICER','STUDENT_READ'),
    ('AICTE_OFFICER','ENROLMENT_READ'), ('AICTE_OFFICER','IMPORT_READ'),
    ('AICTE_OFFICER','CONFLICT_READ'), ('AICTE_OFFICER','CONFLICT_INVESTIGATE'),
    ('AICTE_OFFICER','COMPLIANCE_READ'), ('AICTE_OFFICER','COMPLIANCE_RUN'),
    ('AICTE_OFFICER','ANOMALY_READ'), ('AICTE_OFFICER','ANOMALY_REVIEW'),
    ('AICTE_OFFICER','CASE_READ'), ('AICTE_OFFICER','CASE_DECIDE'),
    ('AICTE_OFFICER','VERIFICATION_READ'), ('AICTE_OFFICER','CERTIFICATE_READ'),
    ('AICTE_OFFICER','DASHBOARD_READ'), ('AICTE_OFFICER','REPORT_EXPORT'),
    ('AICTE_OFFICER','AUDIT_READ'),

    ('INSTITUTION_ADMIN','INSTITUTION_READ'), ('INSTITUTION_ADMIN','INSTITUTION_WRITE'),
    ('INSTITUTION_ADMIN','PROGRAMME_READ'), ('INSTITUTION_ADMIN','PROGRAMME_WRITE'),
    ('INSTITUTION_ADMIN','ACADEMIC_YEAR_READ'), ('INSTITUTION_ADMIN','STUDENT_READ'),
    ('INSTITUTION_ADMIN','STUDENT_WRITE'), ('INSTITUTION_ADMIN','ENROLMENT_READ'),
    ('INSTITUTION_ADMIN','ENROLMENT_WRITE'), ('INSTITUTION_ADMIN','IMPORT_READ'),
    ('INSTITUTION_ADMIN','IMPORT_WRITE'), ('INSTITUTION_ADMIN','APAAR_VALIDATE'),
    ('INSTITUTION_ADMIN','APAAR_VERIFY'), ('INSTITUTION_ADMIN','CONFLICT_READ'),
    ('INSTITUTION_ADMIN','COMPLIANCE_READ'), ('INSTITUTION_ADMIN','COMPLIANCE_RUN'),
    ('INSTITUTION_ADMIN','ANOMALY_READ'), ('INSTITUTION_ADMIN','CASE_READ'),
    ('INSTITUTION_ADMIN','CASE_WRITE'), ('INSTITUTION_ADMIN','VERIFICATION_ISSUE'),
    ('INSTITUTION_ADMIN','VERIFICATION_READ'), ('INSTITUTION_ADMIN','CERTIFICATE_READ'),
    ('INSTITUTION_ADMIN','DASHBOARD_READ'), ('INSTITUTION_ADMIN','REPORT_EXPORT'),

    ('INSTITUTION_OPERATOR','PROGRAMME_READ'), ('INSTITUTION_OPERATOR','ACADEMIC_YEAR_READ'),
    ('INSTITUTION_OPERATOR','STUDENT_READ'), ('INSTITUTION_OPERATOR','STUDENT_WRITE'),
    ('INSTITUTION_OPERATOR','ENROLMENT_READ'), ('INSTITUTION_OPERATOR','ENROLMENT_WRITE'),
    ('INSTITUTION_OPERATOR','IMPORT_READ'), ('INSTITUTION_OPERATOR','IMPORT_WRITE'),
    ('INSTITUTION_OPERATOR','APAAR_VALIDATE'), ('INSTITUTION_OPERATOR','CONFLICT_READ'),
    ('INSTITUTION_OPERATOR','COMPLIANCE_READ'), ('INSTITUTION_OPERATOR','ANOMALY_READ'),
    ('INSTITUTION_OPERATOR','CASE_READ'), ('INSTITUTION_OPERATOR','DASHBOARD_READ'),

    ('VERIFICATION_OFFICER','STUDENT_READ'), ('VERIFICATION_OFFICER','ENROLMENT_READ'),
    ('VERIFICATION_OFFICER','APAAR_VALIDATE'), ('VERIFICATION_OFFICER','APAAR_VERIFY'),
    ('VERIFICATION_OFFICER','VERIFICATION_ISSUE'), ('VERIFICATION_OFFICER','VERIFICATION_READ'),
    ('VERIFICATION_OFFICER','CERTIFICATE_READ'), ('VERIFICATION_OFFICER','DASHBOARD_READ'),

    ('CASE_REVIEWER','STUDENT_READ'), ('CASE_REVIEWER','ENROLMENT_READ'),
    ('CASE_REVIEWER','CONFLICT_READ'), ('CASE_REVIEWER','CONFLICT_INVESTIGATE'),
    ('CASE_REVIEWER','COMPLIANCE_READ'), ('CASE_REVIEWER','ANOMALY_READ'),
    ('CASE_REVIEWER','ANOMALY_REVIEW'), ('CASE_REVIEWER','CASE_READ'),
    ('CASE_REVIEWER','CASE_WRITE'), ('CASE_REVIEWER','CASE_DECIDE'),
    ('CASE_REVIEWER','VERIFICATION_READ'), ('CASE_REVIEWER','DASHBOARD_READ'),

    ('AUDITOR','INSTITUTION_READ'), ('AUDITOR','PROGRAMME_READ'),
    ('AUDITOR','ACADEMIC_YEAR_READ'), ('AUDITOR','STUDENT_READ'),
    ('AUDITOR','ENROLMENT_READ'), ('AUDITOR','IMPORT_READ'),
    ('AUDITOR','CONFLICT_READ'), ('AUDITOR','COMPLIANCE_READ'),
    ('AUDITOR','ANOMALY_READ'), ('AUDITOR','CASE_READ'),
    ('AUDITOR','VERIFICATION_READ'), ('AUDITOR','CERTIFICATE_READ'),
    ('AUDITOR','DASHBOARD_READ'), ('AUDITOR','REPORT_EXPORT'),
    ('AUDITOR','AUDIT_READ')
) AS x(role_code, permission_code) ON x.role_code = r.code;

INSERT INTO admission_rules
    (code, name, description, rule_type, category, config, severity, source_reference)
VALUES
    ('INTAKE_EXCESS',
     'Actual enrolment exceeds approved intake',
     'Flags a programme/academic-year where active enrolments exceed the configured approved intake. Configuration-driven; not an official regulation.',
     'INTAKE_LIMIT', 'ADMISSION', '{"comparison":"ACTUAL_GT_APPROVED"}'::jsonb, 'HIGH',
     'CONFIGURABLE_BASELINE_NOT_OFFICIAL'),
    ('CATEGORY_CAPACITY',
     'Category enrolment exceeds configured category capacity',
     'Flags category-level enrolment above configured seats. Requires institution-specific category capacities from a verified source.',
     'CATEGORY_CAPACITY', 'ADMISSION', '{"includeCategories":["GENERAL","OBC","SC","ST","EWS"]}'::jsonb, 'HIGH',
     'CONFIGURABLE_BASELINE_NOT_OFFICIAL'),
    ('DUPLICATE_ACTIVE_ENROLMENT',
     'Active duplicate enrolment across institutions',
     'Flags a student identity with more than one active enrolment in the same academic year at different institutions. Historical enrolments alone are not a finding.',
     'DUPLICATE_ENROLMENT', 'ADMISSION', '{"compare":"STUDENT_ACADEMIC_YEAR"}'::jsonb, 'CRITICAL',
     'CONFIGURABLE_BASELINE_NOT_OFFICIAL'),
    ('APAAR_FORMAT_INVALID',
     'APAAR identifier fails format validation',
     'Flags records whose supplied APAAR identifier fails the documented format check. Format validity is not authoritative verification.',
     'APAAR_FORMAT', 'IDENTITY', '{"pattern":"^[0-9]{12}$"}'::jsonb, 'MEDIUM',
     'CONFIGURABLE_BASELINE_NOT_OFFICIAL'),
    ('APAAR_NOT_VERIFIED',
     'APAAR identifier not authoritatively verified',
     'Flags records without a successful authoritative verification. Simulated verification never satisfies this rule in production mode.',
     'APAAR_VERIFICATION', 'IDENTITY', '{"allowSimulated":false}'::jsonb, 'LOW',
     'CONFIGURABLE_BASELINE_NOT_OFFICIAL'),
    ('MISSING_REQUIRED_FIELDS',
     'Required admission fields missing',
     'Flags enrolments missing mandatory data needed for regulatory reporting.',
     'DATA_COMPLETENESS', 'DATA_QUALITY', '{"fields":["enrolmentNumber","admissionDate","admissionCategory"]}'::jsonb, 'MEDIUM',
     'CONFIGURABLE_BASELINE_NOT_OFFICIAL');

INSERT INTO retention_policies (entity_type, retention_years, action, legal_basis_note) VALUES
    ('audit_events', 8, 'RETAIN', 'Audit evidence retained; deletion requires a recorded legal decision.'),
    ('case_evidence', 8, 'RETAIN', 'Investigation evidence retained for auditability.'),
    ('verification_events', 5, 'ARCHIVE', 'Verification access history archived after the retention period.'),
    ('students', 10, 'ANONYMIZE', 'Identity data minimised after academic/regulatory retention; policy configurable, not legal advice.'),
    ('enrolments', 10, 'ARCHIVE', 'Enrolment history retained for regulatory reporting/audit.'),
    ('data_import_jobs', 3, 'ARCHIVE', 'Raw imports archived; validation errors retained for audit.'),
    ('notifications', 1, 'DELETE', 'Operational notifications are not evidentiary records.');
