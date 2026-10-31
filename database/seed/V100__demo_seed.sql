-- ===========================================================================
-- DEMO-ONLY synthetic seed data (Spring profile "demo").
--
-- Every institution, person, APAAR identifier and result here is synthetic.
-- Demo accounts share the local-only password documented in README.md.
-- Do not apply this migration to production. Do not reuse any value.
-- ===========================================================================

INSERT INTO institutions (id, code, name, aicte_approval_ref, institution_type, address_line, city, district, state, pincode, contact_email, contact_phone, status) VALUES
    ('11111111-1111-4111-8111-111111111111', 'SIT-DEMO', 'Sunrise Institute of Technology (Demo)', 'DEMO/AICTE/APP/0001', 'PRIVATE', '12 Knowledge Park Road', 'Pune', 'Pune', 'Maharashtra', '411045', 'office@sunrise.demo', '+91-00000-00001', 'ACTIVE'),
    ('22222222-2222-4222-8222-222222222222', 'NCE-DEMO', 'National College of Engineering (Demo)', 'DEMO/AICTE/APP/0002', 'AUTONOMOUS', '45 Ring Road', 'Bengaluru', 'Bengaluru Urban', 'Karnataka', '560001', 'office@national.demo', '+91-00000-00002', 'ACTIVE'),
    ('33333333-3333-4333-8333-333333333333', 'CIM-DEMO', 'Coastal Institute of Management (Demo)', 'DEMO/AICTE/APP/0003', 'AFFILIATED', '8 Harbour Avenue', 'Kochi', 'Ernakulam', 'Kerala', '682001', 'office@coastal.demo', '+91-00000-00003', 'ACTIVE');

INSERT INTO academic_years (id, code, start_date, end_date, active) VALUES
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', '2026-27', '2026-07-01', '2027-06-30', true);

INSERT INTO programmes (id, institution_id, code, name, degree_level, duration_years, status) VALUES
    ('11111111-aaaa-4aaa-8aaa-000000000001', '11111111-1111-4111-8111-111111111111', 'CSE', 'B.Tech Computer Science and Engineering', 'UG', 4.0, 'ACTIVE'),
    ('11111111-aaaa-4aaa-8aaa-000000000002', '11111111-1111-4111-8111-111111111111', 'ECE', 'B.Tech Electronics and Communication Engineering', 'UG', 4.0, 'ACTIVE'),
    ('22222222-aaaa-4aaa-8aaa-000000000001', '22222222-2222-4222-8222-222222222222', 'CSE', 'B.Tech Computer Science and Engineering', 'UG', 4.0, 'ACTIVE'),
    ('33333333-aaaa-4aaa-8aaa-000000000001', '33333333-3333-4333-8333-333333333333', 'MBA', 'Master of Business Administration', 'PG', 2.0, 'ACTIVE');

INSERT INTO programme_intakes (id, programme_id, academic_year_id, approved_intake, supernumerary_intake, nri_intake, oci_intake, foreign_national_intake, status) VALUES
    ('11111111-bbbb-4bbb-8bbb-000000000001', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 60, 3, 3, 1, 2, 'ACTIVE'),
    ('11111111-bbbb-4bbb-8bbb-000000000002', '11111111-aaaa-4aaa-8aaa-000000000002', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 40, 2, 2, 1, 1, 'ACTIVE'),
    ('22222222-bbbb-4bbb-8bbb-000000000001', '22222222-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 50, 2, 2, 1, 1, 'ACTIVE'),
    ('33333333-bbbb-4bbb-8bbb-000000000001', '33333333-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 30, 1, 1, 1, 1, 'ACTIVE');

INSERT INTO programme_intake_categories (programme_intake_id, category, approved_seats) VALUES
    ('11111111-bbbb-4bbb-8bbb-000000000001', 'GENERAL', 30),
    ('11111111-bbbb-4bbb-8bbb-000000000001', 'OBC', 15),
    ('11111111-bbbb-4bbb-8bbb-000000000001', 'SC', 8),
    ('11111111-bbbb-4bbb-8bbb-000000000001', 'ST', 4),
    ('11111111-bbbb-4bbb-8bbb-000000000001', 'EWS', 3),
    ('11111111-bbbb-4bbb-8bbb-000000000002', 'GENERAL', 20),
    ('11111111-bbbb-4bbb-8bbb-000000000002', 'OBC', 10),
    ('11111111-bbbb-4bbb-8bbb-000000000002', 'SC', 6),
    ('11111111-bbbb-4bbb-8bbb-000000000002', 'ST', 4);

-- Local-only demo password: Demo@12345 (bcrypt cost 10)
INSERT INTO users (id, email, password_hash, full_name, institution_id, status) VALUES
    ('00000000-0000-4000-8000-000000000001', 'admin@apaar.local', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'Platform Administrator (Demo)', NULL, 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000002', 'officer@aicte.local', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'AICTE Regulatory Officer (Demo)', NULL, 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000003', 'admin@sunrise.demo', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'Sunrise Institution Administrator (Demo)', '11111111-1111-4111-8111-111111111111', 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000004', 'operator@sunrise.demo', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'Sunrise Data Operator (Demo)', '11111111-1111-4111-8111-111111111111', 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000005', 'reviewer@apaar.local', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'Case Reviewer (Demo)', NULL, 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000006', 'auditor@apaar.local', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'Auditor (Demo)', NULL, 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000007', 'verifier@sunrise.demo', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'Sunrise Verification Officer (Demo)', '11111111-1111-4111-8111-111111111111', 'ACTIVE'),
    ('00000000-0000-4000-8000-000000000008', 'admin@national.demo', '$2b$10$mVPPNAI26gIcDu/05rX/n.lGsi5IgwJ9OokFvlRotx0CXfQsl9Pj2', 'National Institution Administrator (Demo)', '22222222-2222-4222-8222-222222222222', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM (VALUES
    ('00000000-0000-4000-8000-000000000001'::uuid, 'PLATFORM_ADMIN'),
    ('00000000-0000-4000-8000-000000000002'::uuid, 'AICTE_OFFICER'),
    ('00000000-0000-4000-8000-000000000003'::uuid, 'INSTITUTION_ADMIN'),
    ('00000000-0000-4000-8000-000000000004'::uuid, 'INSTITUTION_OPERATOR'),
    ('00000000-0000-4000-8000-000000000005'::uuid, 'CASE_REVIEWER'),
    ('00000000-0000-4000-8000-000000000006'::uuid, 'AUDITOR'),
    ('00000000-0000-4000-8000-000000000007'::uuid, 'VERIFICATION_OFFICER'),
    ('00000000-0000-4000-8000-000000000008'::uuid, 'INSTITUTION_ADMIN')
) AS x(user_id, role_code) JOIN users u ON u.id = x.user_id JOIN roles r ON r.code = x.role_code;

INSERT INTO institution_memberships (user_id, institution_id, role_code) VALUES
    ('00000000-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'INSTITUTION_ADMIN'),
    ('00000000-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'INSTITUTION_OPERATOR'),
    ('00000000-0000-4000-8000-000000000007', '11111111-1111-4111-8111-111111111111', 'VERIFICATION_OFFICER'),
    ('00000000-0000-4000-8000-000000000008', '22222222-2222-4222-8222-222222222222', 'INSTITUTION_ADMIN');

INSERT INTO students (id, apaar_id, apaar_format_valid, apaar_format_validated_at, full_name, date_of_birth, gender, category, email, phone, guardian_name, state, created_by) VALUES
    ('10000000-0000-4000-8000-000000000001', '900000000001', true, now(), 'Aarav Sharma (Synthetic)', '2007-03-14', 'MALE', 'GENERAL', 'aarav.synthetic@example.invalid', '+91-90000-00001', 'Guardian One (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000002', '900000000002', true, now(), 'Diya Patel (Synthetic)', '2007-08-21', 'FEMALE', 'OBC', 'diya.synthetic@example.invalid', '+91-90000-00002', 'Guardian Two (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000003', '12345', false, now(), 'Rohan Mehta (Synthetic)', '2007-01-09', 'MALE', 'GENERAL', 'rohan.synthetic@example.invalid', '+91-90000-00003', 'Guardian Three (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000004', '900000000004', true, now(), 'Isha Nair (Synthetic)', '2007-11-30', 'FEMALE', 'SC', 'isha.synthetic@example.invalid', '+91-90000-00004', 'Guardian Four (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000005', '900000000005', true, now(), 'Kabir Singh (Synthetic)', '2007-05-05', 'MALE', 'ST', 'kabir.synthetic@example.invalid', '+91-90000-00005', 'Guardian Five (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000006', '900000000006', true, now(), 'Ananya Rao (Synthetic)', '2007-09-17', 'FEMALE', 'EWS', 'ananya.synthetic@example.invalid', '+91-90000-00006', 'Guardian Six (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000007', '900000000007', true, now(), 'Vivaan Gupta (Synthetic)', '2007-07-12', 'MALE', 'GENERAL', 'vivaan.synthetic@example.invalid', '+91-90000-00007', 'Guardian Seven (Synthetic)', 'Maharashtra', '00000000-0000-4000-8000-000000000004'),
    ('10000000-0000-4000-8000-000000000008', '900000000008', true, now(), 'Meera Iyer (Synthetic)', '2007-04-03', 'FEMALE', 'OBC', 'meera.synthetic@example.invalid', '+91-90000-00008', 'Guardian Eight (Synthetic)', 'Karnataka', '00000000-0000-4000-8000-000000000008'),
    ('10000000-0000-4000-8000-000000000009', '900000000009', true, now(), 'Arjun Das (Synthetic)', '2007-12-24', 'MALE', 'GENERAL', 'arjun.synthetic@example.invalid', '+91-90000-00009', 'Guardian Nine (Synthetic)', 'Karnataka', '00000000-0000-4000-8000-000000000008'),
    ('10000000-0000-4000-8000-000000000010', '900000000010', true, now(), 'Sara Khan (Synthetic)', '2004-02-16', 'FEMALE', 'GENERAL', 'sara.synthetic@example.invalid', '+91-90000-00010', 'Guardian Ten (Synthetic)', 'Kerala', NULL),
    ('10000000-0000-4000-8000-000000000011', NULL, false, NULL, 'Nikhil Verma (Synthetic)', '2004-06-19', 'MALE', 'SC', 'nikhil.synthetic@example.invalid', '+91-90000-00011', 'Guardian Eleven (Synthetic)', 'Kerala', NULL),
    ('10000000-0000-4000-8000-000000000012', '900000000012', true, now(), 'Priya Menon (Synthetic)', '2004-10-08', 'FEMALE', 'OBC', 'priya.synthetic@example.invalid', '+91-90000-00012', 'Guardian Twelve (Synthetic)', 'Kerala', NULL);

INSERT INTO data_import_jobs (id, institution_id, academic_year_id, import_type, original_filename, content_type, file_checksum, status, total_rows, valid_rows, error_rows, uploaded_by, started_at, completed_at) VALUES
    ('44444444-4444-4444-8444-444444444444', '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'ENROLMENTS', 'sunrise-2026-27-enrolments.csv', 'text/csv', '0000000000000000000000000000000000000000000000000000000000000000', 'COMPLETED', 8, 8, 0, '00000000-0000-4000-8000-000000000004', now() - interval '10 days', now() - interval '10 days');

INSERT INTO enrolments (id, student_id, institution_id, programme_id, academic_year_id, enrolment_number, admission_category, admission_date, status, source_import_id, created_by) VALUES
    ('eeeeeeee-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026CSE001', 'GENERAL', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '22222222-2222-4222-8222-222222222222', '22222222-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'NCE2026CSE001', 'GENERAL', '2026-07-20', 'ACTIVE', NULL, '00000000-0000-4000-8000-000000000008'),
    ('eeeeeeee-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026CSE002', 'OBC', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026CSE003', 'GENERAL', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026CSE004', 'SC', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000005', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026CSE005', 'ST', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000006', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026CSE006', 'EWS', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000007', '11111111-1111-4111-8111-111111111111', '11111111-aaaa-4aaa-8aaa-000000000002', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'SIT2026ECE001', 'GENERAL', '2026-07-15', 'ACTIVE', '44444444-4444-4444-8444-444444444444', '00000000-0000-4000-8000-000000000004'),
    ('eeeeeeee-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000008', '22222222-2222-4222-8222-222222222222', '22222222-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'NCE2026CSE002', 'OBC', '2026-07-20', 'ACTIVE', NULL, '00000000-0000-4000-8000-000000000008'),
    ('eeeeeeee-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000009', '22222222-2222-4222-8222-222222222222', '22222222-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'NCE2026CSE003', 'GENERAL', '2026-07-20', 'ACTIVE', NULL, '00000000-0000-4000-8000-000000000008'),
    ('eeeeeeee-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000010', '33333333-3333-4333-8333-333333333333', '33333333-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'CIM2026MBA001', 'GENERAL', '2026-07-25', 'ACTIVE', NULL, NULL),
    ('eeeeeeee-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000011', '33333333-3333-4333-8333-333333333333', '33333333-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'CIM2026MBA002', 'SC', '2026-07-25', 'ACTIVE', NULL, NULL),
    ('eeeeeeee-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000012', '33333333-3333-4333-8333-333333333333', '33333333-aaaa-4aaa-8aaa-000000000001', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'CIM2026MBA003', 'OBC', '2026-07-25', 'ACTIVE', NULL, NULL);

INSERT INTO apaar_format_validations (student_id, apaar_id, is_valid, reason_code, validated_by, validated_at) VALUES
    ('10000000-0000-4000-8000-000000000002', '900000000002', true, 'FORMAT_VALID', '00000000-0000-4000-8000-000000000004', now() - interval '9 days'),
    ('10000000-0000-4000-8000-000000000003', '12345', false, 'INVALID_LENGTH', '00000000-0000-4000-8000-000000000004', now() - interval '9 days');

INSERT INTO apaar_verification_records (student_id, apaar_id_submitted, verification_mode, adapter_name, status, provider_reference, reason_code, response_payload, requested_by, requested_at, verified_at, expires_at) VALUES
    ('10000000-0000-4000-8000-000000000002', '900000000002', 'SIMULATED', 'simulated-apaar-adapter', 'VERIFIED', 'SIM-000000000002', 'SIMULATED_MATCH', '{"simulated":true,"message":"Synthetic demo adapter - not authoritative"}'::jsonb, '00000000-0000-4000-8000-000000000004', now() - interval '9 days', now() - interval '9 days', now() + interval '1 year'),
    ('10000000-0000-4000-8000-000000000004', '900000000004', 'SIMULATED', 'simulated-apaar-adapter', 'VERIFIED', 'SIM-000000000004', 'SIMULATED_MATCH', '{"simulated":true,"message":"Synthetic demo adapter - not authoritative"}'::jsonb, '00000000-0000-4000-8000-000000000004', now() - interval '8 days', now() - interval '8 days', now() + interval '1 year'),
    ('10000000-0000-4000-8000-000000000005', '900000000005', 'SIMULATED', 'simulated-apaar-adapter', 'NOT_VERIFIED', NULL, 'SIMULATED_NO_MATCH', '{"simulated":true,"message":"Synthetic demo adapter could not confirm"}'::jsonb, '00000000-0000-4000-8000-000000000004', now() - interval '8 days', now() - interval '8 days', NULL);

INSERT INTO academic_records (id, enrolment_id, academic_year_id, semester, credits_earned, credits_total, sgpa, cgpa, result_status, published_at, source, created_by) VALUES
    ('cccccccc-0000-4000-8000-000000000001', 'eeeeeeee-0000-4000-8000-000000000003', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 1, 22, 24, 8.75, 8.75, 'PASS', now() - interval '5 days', 'INSTITUTION_SUBMISSION', '00000000-0000-4000-8000-000000000004'),
    ('cccccccc-0000-4000-8000-000000000002', 'eeeeeeee-0000-4000-8000-000000000005', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 1, 20, 24, 7.90, 7.90, 'PASS', now() - interval '5 days', 'INSTITUTION_SUBMISSION', '00000000-0000-4000-8000-000000000004'),
    ('cccccccc-0000-4000-8000-000000000003', 'eeeeeeee-0000-4000-8000-000000000011', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 1, 18, 20, 8.10, 8.10, 'PASS', now() - interval '4 days', 'INSTITUTION_SUBMISSION', NULL);

INSERT INTO verification_references (id, public_ref, academic_record_id, enrolment_id, purpose, status, issued_by, issued_at, expires_at, max_uses, use_count) VALUES
    ('dddddddd-0000-4000-8000-000000000001', 'demo-verify-2026-synthetic-0001', 'cccccccc-0000-4000-8000-000000000001', 'eeeeeeee-0000-4000-8000-000000000003', 'Demo employer verification', 'ACTIVE', '00000000-0000-4000-8000-000000000007', now() - interval '2 days', now() + interval '180 days', 10, 1);

INSERT INTO verification_events (verification_reference_id, event_type, actor_type, actor_id, result, details) VALUES
    ('dddddddd-0000-4000-8000-000000000001', 'PUBLIC_VERIFICATION', 'PUBLIC', NULL, 'SUCCESS', '{"minimalDisclosure":true}'::jsonb);

INSERT INTO identity_conflicts (id, conflict_type, primary_student_id, secondary_student_id, primary_enrolment_id, secondary_enrolment_id, detection_source, status, severity, reason_code, details, assigned_to) VALUES
    ('ffffffff-0000-4000-8000-000000000001', 'CROSS_INSTITUTION_ACTIVE_ENROLMENT', '10000000-0000-4000-8000-000000000001', NULL, 'eeeeeeee-0000-4000-8000-000000000001', 'eeeeeeee-0000-4000-8000-000000000002', 'RULE', 'OPEN', 'HIGH', 'DUPLICATE_ACTIVE_ENROLMENT', '{"academicYear":"2026-27","institutions":["SIT-DEMO","NCE-DEMO"]}'::jsonb, '00000000-0000-4000-8000-000000000005');

INSERT INTO compliance_evaluations (id, institution_id, academic_year_id, status, ruleset_version, evaluated_by, evaluated_at, total_findings, summary) VALUES
    ('99999999-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'COMPLETED', 1, '00000000-0000-4000-8000-000000000002', now() - interval '3 days', 2, '{"rulesEvaluated":6,"enrolmentsEvaluated":8}'::jsonb);

INSERT INTO compliance_violations (evaluation_id, rule_id, enrolment_id, student_id, reason_code, severity, details, status) VALUES
    ('99999999-0000-4000-8000-000000000001', (SELECT id FROM admission_rules WHERE code = 'APAAR_FORMAT_INVALID'), 'eeeeeeee-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000003', 'APAAR_FORMAT_INVALID', 'MEDIUM', '{"apaarId":"12345"}'::jsonb, 'OPEN'),
    ('99999999-0000-4000-8000-000000000001', (SELECT id FROM admission_rules WHERE code = 'APAAR_NOT_VERIFIED'), 'eeeeeeee-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000005', 'APAAR_NOT_VERIFIED', 'LOW', '{"latestStatus":"NOT_VERIFIED"}'::jsonb, 'OPEN');

INSERT INTO anomaly_runs (id, institution_id, academic_year_id, model_name, model_version, requested_by, status, records_evaluated, flagged_count, completed_at) VALUES
    ('88888888-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'apaar-regulatory-anomaly', '0.1.0', '00000000-0000-4000-8000-000000000002', 'COMPLETED', 8, 1, now() - interval '2 days');

INSERT INTO anomaly_results (id, run_id, entity_type, entity_id, student_id, enrolment_id, model_name, model_version, anomaly_score, threshold, is_anomaly, severity, reason_codes, explanations, features, status) VALUES
    ('77777777-0000-4000-8000-000000000001', '88888888-0000-4000-8000-000000000001', 'ENROLMENT', 'eeeeeeee-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'eeeeeeee-0000-4000-8000-000000000001', 'apaar-regulatory-anomaly', '0.1.0', 0.8700, 0.6500, true, 'HIGH', ARRAY['DUPLICATE_ENROLMENT_SIGNAL','APAAR_UNVERIFIED'], '[{"code":"DUPLICATE_ENROLMENT_SIGNAL","message":"Student identity has another active enrolment in the same academic year.","contribution":0.55}]'::jsonb, '{"duplicate_enrolment_count":1,"apaar_verified":false}'::jsonb, 'NEW');

INSERT INTO investigation_cases (id, case_number, institution_id, case_type, title, description, priority, status, assigned_to, opened_by, opened_at, due_at) VALUES
    ('66666666-0000-4000-8000-000000000001', 'CASE-2026-0001', NULL, 'IDENTITY_CONFLICT', 'Cross-institution active enrolment for synthetic student', 'Synthetic demo case: one student identity has active enrolments at two institutions in 2026-27. Investigate whether this is a data-entry error, legitimate transfer or duplicate identity.', 'HIGH', 'OPEN', '00000000-0000-4000-8000-000000000005', '00000000-0000-4000-8000-000000000005', now() - interval '2 days', now() + interval '5 days');

INSERT INTO case_links (case_id, entity_type, entity_id) VALUES
    ('66666666-0000-4000-8000-000000000001', 'IDENTITY_CONFLICT', 'ffffffff-0000-4000-8000-000000000001'),
    ('66666666-0000-4000-8000-000000000001', 'ANOMALY', '77777777-0000-4000-8000-000000000001');

INSERT INTO case_comments (case_id, author_id, body, internal) VALUES
    ('66666666-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000005', 'Synthetic demo comment: requested institution clarification before a decision.', true);

INSERT INTO audit_events (event_type, actor_id, actor_role, institution_id, entity_type, entity_id, action, outcome, metadata) VALUES
    ('IMPORT_COMPLETED', '00000000-0000-4000-8000-000000000004', 'INSTITUTION_OPERATOR', '11111111-1111-4111-8111-111111111111', 'DATA_IMPORT_JOB', '44444444-4444-4444-8444-444444444444', 'IMPORT', 'SUCCESS', '{"rows":8,"synthetic":true}'::jsonb),
    ('COMPLIANCE_EVALUATED', '00000000-0000-4000-8000-000000000002', 'AICTE_OFFICER', '11111111-1111-4111-8111-111111111111', 'COMPLIANCE_EVALUATION', '99999999-0000-4000-8000-000000000001', 'EVALUATE', 'SUCCESS', '{"findings":2,"synthetic":true}'::jsonb),
    ('CASE_OPENED', '00000000-0000-4000-8000-000000000005', 'CASE_REVIEWER', NULL, 'INVESTIGATION_CASE', '66666666-0000-4000-8000-000000000001', 'CREATE', 'SUCCESS', '{"synthetic":true}'::jsonb);

INSERT INTO notifications (recipient_user_id, institution_id, type, title, body, severity) VALUES
    ('00000000-0000-4000-8000-000000000005', NULL, 'CASE_ASSIGNED', 'Synthetic case assigned', 'Case CASE-2026-0001 requires review.', 'WARNING'),
    ('00000000-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'COMPLIANCE_FINDING', 'Compliance findings available', '2 synthetic compliance findings require acknowledgement.', 'INFO');
