package com.apaar.platform.ingestion.service;

import com.apaar.platform.apaar.ApaarFormatValidator;
import com.apaar.platform.ingestion.dto.ImportDtos.*;
import com.apaar.platform.student.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SubmissionService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final ApaarFormatValidator apaarFormatValidator;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StudentRecordVersionRepository recordVersionRepository;

    public SubmissionService(
            ApaarFormatValidator apaarFormatValidator,
            StudentRepository studentRepository,
            EnrolmentRepository enrolmentRepository,
            StudentRecordVersionRepository recordVersionRepository
    ) {
        this.apaarFormatValidator = apaarFormatValidator;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.recordVersionRepository = recordVersionRepository;
    }

    @Transactional
    public SingleSubmissionResult submitSingle(SingleSubmissionRequest req, UUID institutionId, UUID userId) {
        List<RowErrorDto> errors = new ArrayList<>();

        if (institutionId == null) {
            errors.add(new RowErrorDto(1, "institutionId", "INSTITUTION_REQUIRED",
                    "An institution must be selected for the submission", ""));
        }
        if (req.studentIdentifier() == null || req.studentIdentifier().isBlank()) {
            errors.add(new RowErrorDto(1, "studentIdentifier", "MISSING_MANDATORY", "Student identifier is mandatory", ""));
        }
        if (req.fullName() == null || req.fullName().isBlank()) {
            errors.add(new RowErrorDto(1, "fullName", "MISSING_MANDATORY", "Full name is mandatory", ""));
        }
        if (req.admissionCategory() == null || req.admissionCategory().isBlank()) {
            errors.add(new RowErrorDto(1, "admissionCategory", "MISSING_MANDATORY", "Admission category is mandatory", ""));
        }

        String studentIdentifier = req.studentIdentifier() != null ? req.studentIdentifier().trim() : null;

        String email = req.email() != null && !req.email().isBlank()
                ? req.email().trim().toLowerCase(Locale.ROOT) : null;
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(new RowErrorDto(1, "email", "INVALID_EMAIL", "Invalid email address format", req.email()));
        }

        String phone = null;
        if (req.phone() != null && !req.phone().isBlank()) {
            phone = req.phone().replaceAll("[^0-9]", "");
            if (phone.length() < 10 || phone.length() > 12) {
                errors.add(new RowErrorDto(1, "phone", "INVALID_PHONE",
                        "Phone number must contain 10-12 digits", req.phone()));
                phone = null;
            }
        }

        boolean apaarValid = false;
        String apaarStatus = "MISSING";
        String apaarId = req.apaarId() != null ? req.apaarId().trim() : null;
        if (apaarId != null && !apaarId.isBlank()) {
            ApaarFormatValidator.Result res = apaarFormatValidator.validate(apaarId);
            if (res.valid()) {
                apaarValid = true;
                apaarStatus = "FORMAT_VALID";
            } else {
                apaarStatus = "FORMAT_INVALID";
                errors.add(new RowErrorDto(1, "apaarId", "INVALID_APAAR_FORMAT", res.message(), req.apaarId()));
            }
        } else {
            apaarId = null;
        }

        String normGender = normalizeGender(req.gender());
        String normCategory = normalizeCategory(req.admissionCategory());
        if (req.admissionCategory() != null && !req.admissionCategory().isBlank() && normCategory == null) {
            errors.add(new RowErrorDto(1, "admissionCategory", "INVALID_CATEGORY",
                    "Unsupported admission category: " + req.admissionCategory(), req.admissionCategory()));
        }

        if (req.dateOfBirth() != null
                && (req.dateOfBirth().isAfter(LocalDate.now().minusYears(10))
                || req.dateOfBirth().isBefore(LocalDate.now().minusYears(100)))) {
            errors.add(new RowErrorDto(1, "dateOfBirth", "IMPLAUSIBLE_DATE",
                    "Date of birth must be a plausible student age", req.dateOfBirth().toString()));
        }

        if (req.admissionDate() != null && req.admissionDate().isAfter(LocalDate.now().plusYears(1))) {
            errors.add(new RowErrorDto(1, "admissionDate", "IMPLAUSIBLE_DATE",
                    "Admission date cannot be in the far future", req.admissionDate().toString()));
        }

        if (!errors.isEmpty()) {
            return new SingleSubmissionResult(false, studentIdentifier, null, null, apaarStatus, errors);
        }

        // Check if enrolment exists
        Optional<Enrolment> existingEnr = enrolmentRepository.findByInstitutionIdAndEnrolmentNumber(institutionId, studentIdentifier);
        Student student;
        Enrolment enrolment;

        if (existingEnr.isPresent()) {
            Enrolment existing = existingEnr.get();
            enrolment = existing;
            UUID enrId = existing.getId();
            student = studentRepository.findById(existing.getStudentId())
                    .orElseThrow(() -> new IllegalStateException("Student not found for enrolment: " + enrId));

            Optional<Student> studentByApaar = (apaarId != null)
                    ? studentRepository.findByApaarId(apaarId) : Optional.empty();

            if (studentByApaar.isPresent() && !studentByApaar.get().getId().equals(student.getId())) {
                Student targetStudent = studentByApaar.get();
                List<Enrolment> existingTargetEnrs = enrolmentRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                        targetStudent.getId(), enrolment.getAcademicYearId(), List.of("ACTIVE", "PROVISIONAL"));
                for (Enrolment other : existingTargetEnrs) {
                    if (!other.getId().equals(enrolment.getId()) && other.getProgrammeId().equals(enrolment.getProgrammeId())) {
                        other.setStatus("WITHDRAWN");
                        enrolmentRepository.saveAndFlush(other);
                    }
                }
                student.setApaarId(null);
                studentRepository.saveAndFlush(student);

                enrolment.setStudentId(targetStudent.getId());
                targetStudent.setFullName(req.fullName());
                if (req.dateOfBirth() != null) targetStudent.setDateOfBirth(req.dateOfBirth());
                if (normGender != null) targetStudent.setGender(normGender);
                if (normCategory != null) targetStudent.setCategory(normCategory);
                if (email != null) targetStudent.setEmail(email);
                if (phone != null) targetStudent.setPhone(phone);
                if (req.guardianName() != null) targetStudent.setGuardianName(req.guardianName());
                if (req.addressLine() != null) targetStudent.setAddressLine(req.addressLine());
                if (req.state() != null) targetStudent.setState(req.state());
                student = studentRepository.saveAndFlush(targetStudent);
                recordVersion(targetStudent, "API_SUBMISSION_RECONCILE", userId);
            } else {
                student.setFullName(req.fullName());
                if (apaarId != null) {
                    student.setApaarId(apaarId);
                    student.setApaarFormatValid(apaarValid);
                    if (apaarValid) student.setApaarFormatValidatedAt(Instant.now());
                }
                if (req.dateOfBirth() != null) student.setDateOfBirth(req.dateOfBirth());
                if (normGender != null) student.setGender(normGender);
                if (normCategory != null) student.setCategory(normCategory);
                if (email != null) student.setEmail(email);
                if (phone != null) student.setPhone(phone);
                if (req.guardianName() != null) student.setGuardianName(req.guardianName());
                if (req.addressLine() != null) student.setAddressLine(req.addressLine());
                if (req.state() != null) student.setState(req.state());
                student = studentRepository.saveAndFlush(student);
                recordVersion(student, "API_SUBMISSION_UPDATE", userId);
            }

            enrolment.setAdmissionCategory(normCategory);
            enrolment.setQuotaType(req.quotaType());
            if (req.admissionDate() != null) enrolment.setAdmissionDate(req.admissionDate());
            enrolmentRepository.saveAndFlush(enrolment);
        } else {
            // Check student by APAAR
            Optional<Student> studentByApaar = (apaarId != null)
                    ? studentRepository.findByApaarId(apaarId) : Optional.empty();

            if (studentByApaar.isPresent()) {
                student = studentByApaar.get();
                student.setFullName(req.fullName());
                if (req.dateOfBirth() != null) student.setDateOfBirth(req.dateOfBirth());
                if (normGender != null) student.setGender(normGender);
                if (normCategory != null) student.setCategory(normCategory);
                if (email != null) student.setEmail(email);
                if (phone != null) student.setPhone(phone);
                if (req.guardianName() != null) student.setGuardianName(req.guardianName());
                if (req.addressLine() != null) student.setAddressLine(req.addressLine());
                if (req.state() != null) student.setState(req.state());
                student = studentRepository.saveAndFlush(student);
                recordVersion(student, "API_SUBMISSION_LINK", userId);
            } else {
                student = new Student();
                student.setFullName(req.fullName());
                student.setApaarId(apaarId);
                student.setApaarFormatValid(apaarValid);
                if (apaarValid) student.setApaarFormatValidatedAt(Instant.now());
                student.setDateOfBirth(req.dateOfBirth());
                student.setGender(normGender);
                student.setCategory(normCategory);
                student.setEmail(email);
                student.setPhone(phone);
                student.setGuardianName(req.guardianName());
                student.setAddressLine(req.addressLine());
                student.setState(req.state());
                student.setCreatedBy(userId);
                student = studentRepository.saveAndFlush(student);
                recordVersion(student, "API_SUBMISSION_INITIAL", userId);
            }

            List<Enrolment> existingStudentEnrs = enrolmentRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                    student.getId(), req.academicYearId(), List.of("ACTIVE", "PROVISIONAL"));

            Optional<Enrolment> existingProgEnr = existingStudentEnrs.stream()
                    .filter(e -> e.getProgrammeId().equals(req.programmeId()))
                    .findFirst();

            if (existingProgEnr.isPresent()) {
                enrolment = existingProgEnr.get();
                for (Enrolment other : existingStudentEnrs) {
                    if (!other.getId().equals(enrolment.getId()) && other.getProgrammeId().equals(req.programmeId())) {
                        other.setStatus("WITHDRAWN");
                        enrolmentRepository.saveAndFlush(other);
                    }
                }
                enrolment.setEnrolmentNumber(studentIdentifier);
                enrolment.setAdmissionCategory(normCategory);
                enrolment.setQuotaType(req.quotaType());
                if (req.admissionDate() != null) enrolment.setAdmissionDate(req.admissionDate());
                enrolment = enrolmentRepository.saveAndFlush(enrolment);
            } else {
                enrolment = new Enrolment();
                enrolment.setStudentId(student.getId());
                enrolment.setInstitutionId(institutionId);
                enrolment.setProgrammeId(req.programmeId());
                enrolment.setAcademicYearId(req.academicYearId());
                enrolment.setEnrolmentNumber(studentIdentifier);
                enrolment.setAdmissionCategory(normCategory);
                enrolment.setQuotaType(req.quotaType());
                enrolment.setAdmissionDate(req.admissionDate() != null ? req.admissionDate() : LocalDate.now());
                enrolment.setStatus("ACTIVE");
                enrolment.setCreatedBy(userId);
                enrolment = enrolmentRepository.saveAndFlush(enrolment);
            }
        }

        return new SingleSubmissionResult(true, studentIdentifier, student.getId(), enrolment.getId(), apaarStatus, Collections.emptyList());
    }

    @Transactional
    public BatchSubmissionResult submitBatch(BatchSubmissionRequest batchReq, UUID institutionId, UUID userId) {
        List<SingleSubmissionResult> results = new ArrayList<>();
        int success = 0;
        int failure = 0;

        for (SingleSubmissionRequest r : batchReq.records()) {
            UUID progId = r.programmeId() != null ? r.programmeId() : batchReq.programmeId();
            UUID yearId = r.academicYearId() != null ? r.academicYearId() : batchReq.academicYearId();
            UUID instId = r.institutionId() != null ? r.institutionId() : institutionId;

            SingleSubmissionRequest effective = new SingleSubmissionRequest(
                    instId, yearId, progId, r.studentIdentifier(), r.apaarId(),
                    r.fullName(), r.dateOfBirth(), r.gender(), r.admissionCategory(),
                    r.admissionDate(), r.email(), r.phone(), r.guardianName(),
                    r.addressLine(), r.state(), r.quotaType(), r.supernumerary(),
                    r.nriClassification(), r.ociClassification(), r.foreignNationalClassification()
            );

            SingleSubmissionResult res = submitSingle(effective, instId, userId);
            results.add(res);
            if (res.success()) {
                success++;
            } else {
                failure++;
            }
        }

        return new BatchSubmissionResult(batchReq.records().size(), success, failure, results);
    }

    private String normalizeGender(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String g = raw.trim().toUpperCase();
        if (g.startsWith("F")) return "FEMALE";
        if (g.startsWith("M")) return "MALE";
        if (g.startsWith("O")) return "OTHER";
        return "PREFER_NOT_TO_SAY";
    }

    private String normalizeCategory(String raw) {
        if (raw == null || raw.isBlank()) return "GENERAL";
        String c = raw.trim().toUpperCase();
        return switch (c) {
            case "OPEN", "GEN", "GENERAL" -> "GENERAL";
            case "OBC", "OBC-NCL" -> "OBC";
            case "SC" -> "SC";
            case "ST" -> "ST";
            case "EWS" -> "EWS";
            case "NRI" -> "NRI";
            case "OCI" -> "OCI";
            case "FOREIGN", "FOREIGN NATIONAL", "FOREIGN-NATIONAL", "FOREIGN_NATIONAL" -> "FOREIGN_NATIONAL";
            case "SUPERNUMERARY" -> "SUPERNUMERARY";
            case "PWD" -> "PWD";
            case "MANAGEMENT" -> "MANAGEMENT";
            default -> null;
        };
    }

    private void recordVersion(Student student, String reason, UUID changedBy) {
        int latestVersion = recordVersionRepository.findLatestVersionNo(student.getId());
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", student.getId());
        snapshot.put("fullName", student.getFullName());
        snapshot.put("apaarId", student.getApaarId());
        snapshot.put("apaarFormatValid", student.isApaarFormatValid());
        snapshot.put("dateOfBirth", student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : null);
        snapshot.put("gender", student.getGender());
        snapshot.put("category", student.getCategory());
        snapshot.put("email", student.getEmail());
        snapshot.put("phone", student.getPhone());
        snapshot.put("timestamp", Instant.now().toString());

        StudentRecordVersion version = new StudentRecordVersion(
                student.getId(),
                latestVersion + 1,
                snapshot,
                reason,
                changedBy
        );
        recordVersionRepository.save(version);
    }
}
