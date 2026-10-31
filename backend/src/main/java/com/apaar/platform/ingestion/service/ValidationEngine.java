package com.apaar.platform.ingestion.service;

import com.apaar.platform.apaar.ApaarFormatValidator;
import com.apaar.platform.ingestion.DataImportError;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ValidationEngine {

    private final ApaarFormatValidator apaarFormatValidator;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    );

    /**
     * Must stay aligned with the students/enrolments category check constraints
     * defined in V4__fix_status_check_constraint.sql.
     */
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "GENERAL", "OPEN", "OBC", "SC", "ST", "EWS", "NRI", "OCI",
            "FOREIGN_NATIONAL", "SUPERNUMERARY", "PWD", "MANAGEMENT"
    );

    public ValidationEngine(ApaarFormatValidator apaarFormatValidator,
                            StudentRepository studentRepository,
                            EnrolmentRepository enrolmentRepository) {
        this.apaarFormatValidator = apaarFormatValidator;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
    }

    public record RowValidationResult(
            String status, // VALID, INVALID, DUPLICATE, REVIEW_REQUIRED
            Map<String, Object> normalizedPayload,
            List<DataImportError> errors,
            UUID matchedEnrolmentId,
            String reconciliationAction // NEW, UNCHANGED, DATA_UPDATE, CONFLICT
    ) {}

    public RowValidationResult validateRow(
            UUID importJobId,
            int rowNumber,
            Map<String, String> mappedRow,
            UUID institutionId,
            UUID academicYearId,
            UUID programmeId,
            Set<String> seenIdentifiersInBatch,
            Set<String> seenApaarInBatch
    ) {
        List<DataImportError> errors = new ArrayList<>();
        Map<String, Object> payload = new HashMap<>();

        // 1. Mandatory Student Identifier / Enrolment Number
        String studentIdentifier = getCleanString(mappedRow, "studentIdentifier");
        if (studentIdentifier == null || studentIdentifier.isBlank()) {
            errors.add(new DataImportError(importJobId, rowNumber, "studentIdentifier", "MISSING_MANDATORY",
                    "Student identifier / enrolment number is required", ""));
        } else {
            payload.put("studentIdentifier", studentIdentifier);
            if (seenIdentifiersInBatch.contains(studentIdentifier.toUpperCase())) {
                errors.add(new DataImportError(importJobId, rowNumber, "studentIdentifier", "DUPLICATE_IN_FILE",
                        "Duplicate student identifier in the same import file: " + studentIdentifier, studentIdentifier));
            } else {
                seenIdentifiersInBatch.add(studentIdentifier.toUpperCase());
            }
        }

        // 2. Mandatory Full Name
        String fullName = getCleanString(mappedRow, "fullName");
        if (fullName == null || fullName.isBlank()) {
            errors.add(new DataImportError(importJobId, rowNumber, "fullName", "MISSING_MANDATORY",
                    "Student full name is mandatory", ""));
        } else {
            payload.put("fullName", fullName);
        }

        // 3. APAAR Format Check (12 digits check; distinct from authoritative verification)
        String rawApaar = getCleanString(mappedRow, "apaarId");
        boolean apaarValid = false;
        String apaarStatus = "MISSING";
        if (rawApaar != null && !rawApaar.isBlank()) {
            ApaarFormatValidator.Result apaarResult = apaarFormatValidator.validate(rawApaar);
            if (apaarResult.valid()) {
                apaarValid = true;
                apaarStatus = "FORMAT_VALID";
                payload.put("apaarId", rawApaar);
                if (seenApaarInBatch.contains(rawApaar)) {
                    errors.add(new DataImportError(importJobId, rowNumber, "apaarId", "DUPLICATE_IN_FILE",
                            "Duplicate APAAR ID in the same import file: " + rawApaar, rawApaar));
                } else {
                    seenApaarInBatch.add(rawApaar);
                }
            } else {
                apaarStatus = "FORMAT_INVALID";
                errors.add(new DataImportError(importJobId, rowNumber, "apaarId", "INVALID_APAAR_FORMAT",
                        apaarResult.message() + " (Note: format check only, not authoritative verification)", rawApaar));
            }
        }
        payload.put("apaarFormatValid", apaarValid);
        payload.put("apaarStatus", apaarStatus);

        // 4. Date of Birth
        String rawDob = getCleanString(mappedRow, "dateOfBirth");
        if (rawDob != null && !rawDob.isBlank()) {
            LocalDate dob = parseDate(rawDob);
            if (dob == null) {
                errors.add(new DataImportError(importJobId, rowNumber, "dateOfBirth", "INVALID_DATE_FORMAT",
                        "Invalid date format for Date of Birth. Expected formats: YYYY-MM-DD or DD/MM/YYYY", rawDob));
            } else if (dob.isAfter(LocalDate.now().minusYears(10)) || dob.isBefore(LocalDate.now().minusYears(100))) {
                errors.add(new DataImportError(importJobId, rowNumber, "dateOfBirth", "IMPLAUSIBLE_DATE",
                        "Date of birth must be a plausible student age", rawDob));
            } else {
                payload.put("dateOfBirth", dob.toString());
            }
        }

        // 5. Gender
        String rawGender = getCleanString(mappedRow, "gender");
        if (rawGender != null && !rawGender.isBlank()) {
            String normalizedGender = normalizeGender(rawGender);
            payload.put("gender", normalizedGender);
        }

        // 6. Mandatory Admission Category
        String rawCategory = getCleanString(mappedRow, "admissionCategory");
        if (rawCategory == null || rawCategory.isBlank()) {
            errors.add(new DataImportError(importJobId, rowNumber, "admissionCategory", "MISSING_MANDATORY",
                    "Admission category is required (e.g. OPEN, OBC, SC, ST, EWS)", ""));
        } else {
            String normalizedCategory = normalizeCategory(rawCategory);
            if (!VALID_CATEGORIES.contains(normalizedCategory)) {
                errors.add(new DataImportError(importJobId, rowNumber, "admissionCategory", "INVALID_CATEGORY",
                        "Unsupported admission category: " + rawCategory, rawCategory));
            } else {
                payload.put("admissionCategory", normalizedCategory);
            }
        }

        // 7. Admission Date
        String rawAdmDate = getCleanString(mappedRow, "admissionDate");
        if (rawAdmDate != null && !rawAdmDate.isBlank()) {
            LocalDate admDate = parseDate(rawAdmDate);
            if (admDate == null) {
                errors.add(new DataImportError(importJobId, rowNumber, "admissionDate", "INVALID_DATE_FORMAT",
                        "Invalid date format for Admission Date", rawAdmDate));
            } else if (admDate.isAfter(LocalDate.now().plusYears(1))) {
                errors.add(new DataImportError(importJobId, rowNumber, "admissionDate", "IMPLAUSIBLE_DATE",
                        "Admission date cannot be in the far future", rawAdmDate));
            } else {
                payload.put("admissionDate", admDate.toString());
            }
        }

        // 8. Contact Details
        String email = getCleanString(mappedRow, "email");
        if (email != null && !email.isBlank()) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.add(new DataImportError(importJobId, rowNumber, "email", "INVALID_EMAIL",
                        "Invalid email address format", email));
            } else {
                payload.put("email", email.toLowerCase());
            }
        }

        String phone = getCleanString(mappedRow, "phone");
        if (phone != null && !phone.isBlank()) {
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.length() < 10 || digits.length() > 12) {
                errors.add(new DataImportError(importJobId, rowNumber, "phone", "INVALID_PHONE",
                        "Phone number must contain 10-12 digits", phone));
            } else {
                payload.put("phone", digits);
            }
        }

        // Optional demographic / quota fields
        putIfPresent(payload, "guardianName", getCleanString(mappedRow, "guardianName"));
        putIfPresent(payload, "addressLine", getCleanString(mappedRow, "addressLine"));
        putIfPresent(payload, "state", getCleanString(mappedRow, "state"));
        putIfPresent(payload, "quotaType", getCleanString(mappedRow, "quotaType"));

        // Boolean classifications
        payload.put("supernumerary", parseBoolean(getCleanString(mappedRow, "supernumerary")));
        payload.put("nriClassification", parseBoolean(getCleanString(mappedRow, "nriClassification")));
        payload.put("ociClassification", parseBoolean(getCleanString(mappedRow, "ociClassification")));
        payload.put("foreignNationalClassification", parseBoolean(getCleanString(mappedRow, "foreignNationalClassification")));

        // 9. Database conflict check & reconciliation classification
        UUID matchedEnrolmentId = null;
        String reconciliationAction = "NEW";

        if (studentIdentifier != null && !studentIdentifier.isBlank()) {
            Optional<Enrolment> existingEnrolmentOpt = enrolmentRepository.findByInstitutionIdAndEnrolmentNumber(
                    institutionId, studentIdentifier);

            if (existingEnrolmentOpt.isPresent()) {
                Enrolment existingEnrolment = existingEnrolmentOpt.get();
                matchedEnrolmentId = existingEnrolment.getId();

                // Check student record
                Optional<Student> existingStudentOpt = studentRepository.findById(existingEnrolment.getStudentId());
                if (existingStudentOpt.isPresent()) {
                    Student existingStudent = existingStudentOpt.get();

                    // Compare values to determine whether it's UNCHANGED or DATA_UPDATE or CONFLICT
                    boolean nameMatches = existingStudent.getFullName().equalsIgnoreCase(fullName != null ? fullName : "");
                    if (nameMatches) {
                        boolean hasChanges = false;
                        if (rawApaar != null && !rawApaar.equals(existingStudent.getApaarId())) hasChanges = true;
                        if (email != null && !email.equalsIgnoreCase(existingStudent.getEmail())) hasChanges = true;
                        if (phone != null && !phone.equals(existingStudent.getPhone())) hasChanges = true;

                        reconciliationAction = hasChanges ? "DATA_UPDATE" : "UNCHANGED";
                    } else {
                        // Same enrolment number but different name -> possible conflict
                        reconciliationAction = "CONFLICT";
                        errors.add(new DataImportError(importJobId, rowNumber, "studentIdentifier", "IDENTITY_CONFLICT",
                                "Enrolment number already assigned to different student: " + existingStudent.getFullName(),
                                studentIdentifier));
                    }
                }
            }
        }

        // Determine final row status
        String status;
        boolean hasDuplicateError = errors.stream().anyMatch(e -> e.getErrorCode().contains("DUPLICATE"));
        if (hasDuplicateError) {
            status = "DUPLICATE";
        } else if (!errors.isEmpty()) {
            status = "INVALID";
        } else if ("CONFLICT".equals(reconciliationAction)) {
            status = "REVIEW_REQUIRED";
        } else {
            status = "VALID";
        }

        return new RowValidationResult(status, payload, errors, matchedEnrolmentId, reconciliationAction);
    }

    private String getCleanString(Map<String, String> row, String key) {
        String val = row.get(key);
        return val != null ? val.trim() : null;
    }

    private void putIfPresent(Map<String, Object> map, String key, String val) {
        if (val != null && !val.isBlank()) {
            map.put(key, val);
        }
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(val.trim(), fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private String normalizeGender(String raw) {
        String clean = raw.trim().toUpperCase();
        if (clean.startsWith("M")) return "MALE";
        if (clean.startsWith("F")) return "FEMALE";
        if (clean.startsWith("T") || clean.startsWith("O") || clean.contains("OTHER")) return "OTHER";
        return "PREFER_NOT_TO_SAY";
    }

    private String normalizeCategory(String raw) {
        String clean = raw.trim().toUpperCase(Locale.ROOT);
        return switch (clean) {
            case "OPEN", "GEN", "GEN.", "GENERAL" -> "GENERAL";
            case "OBC-NCL", "OBC_NCL" -> "OBC";
            case "FOREIGN", "FOREIGN NATIONAL", "FOREIGN-NATIONAL" -> "FOREIGN_NATIONAL";
            default -> clean;
        };
    }

    private boolean parseBoolean(String val) {
        if (val == null) return false;
        String lower = val.trim().toLowerCase();
        return lower.equals("true") || lower.equals("yes") || lower.equals("1") || lower.equals("y");
    }
}

