-- Fix check constraint on data_import_jobs to include VALIDATED_WITH_ERRORS
ALTER TABLE data_import_jobs DROP CONSTRAINT IF EXISTS data_import_jobs_status_ck;
ALTER TABLE data_import_jobs ADD CONSTRAINT data_import_jobs_status_ck CHECK (status IN (
    'RECEIVED', 'UPLOADED', 'AWAITING_MAPPING', 'VALIDATING', 'VALIDATED',
    'VALIDATED_WITH_ERRORS', 'VALIDATION_COMPLETED', 'READY_FOR_COMMIT',
    'PROCESSING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED', 'CANCELLED'
));

-- Allow both OPEN and GENERAL, plus PWD, MANAGEMENT in categories
ALTER TABLE students DROP CONSTRAINT IF EXISTS students_category_ck;
ALTER TABLE students ADD CONSTRAINT students_category_ck CHECK (category IS NULL OR (category IN (
    'GENERAL', 'OPEN', 'OBC', 'SC', 'ST', 'EWS', 'NRI', 'OCI', 'FOREIGN_NATIONAL', 'SUPERNUMERARY', 'PWD', 'MANAGEMENT'
)));

ALTER TABLE enrolments DROP CONSTRAINT IF EXISTS enrolments_category_ck;
ALTER TABLE enrolments ADD CONSTRAINT enrolments_category_ck CHECK (admission_category IN (
    'GENERAL', 'OPEN', 'OBC', 'SC', 'ST', 'EWS', 'NRI', 'OCI', 'FOREIGN_NATIONAL', 'SUPERNUMERARY', 'PWD', 'MANAGEMENT'
));
