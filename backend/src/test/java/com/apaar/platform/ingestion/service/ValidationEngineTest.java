package com.apaar.platform.ingestion.service;

import com.apaar.platform.apaar.ApaarFormatValidator;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationEngineTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    private ValidationEngine validationEngine;
    private final UUID importJobId = UUID.randomUUID();
    private final UUID institutionId = UUID.randomUUID();
    private final UUID academicYearId = UUID.randomUUID();
    private final UUID programmeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ApaarFormatValidator formatValidator = new ApaarFormatValidator();
        validationEngine = new ValidationEngine(formatValidator, studentRepository, enrolmentRepository);
    }

    @Test
    @DisplayName("Valid row with complete attributes passes validation")
    void testValidRow() {
        Map<String, String> row = Map.of(
                "studentIdentifier", "STU-2024-001",
                "fullName", "Rohan Mehta",
                "apaarId", "123456789012",
                "dateOfBirth", "2003-05-15",
                "gender", "MALE",
                "admissionCategory", "OPEN",
                "admissionDate", "2024-08-01",
                "email", "rohan.mehta@example.edu.in",
                "phone", "9876543210"
        );

        Set<String> seenIdentifiers = new HashSet<>();
        Set<String> seenApaar = new HashSet<>();

        ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                importJobId, 1, row, institutionId, academicYearId, programmeId, seenIdentifiers, seenApaar
        );

        assertThat(res.status()).isEqualTo("VALID");
        assertThat(res.errors()).isEmpty();
        assertThat(res.normalizedPayload().get("apaarFormatValid")).isEqualTo(true);
        assertThat(res.normalizedPayload().get("apaarStatus")).isEqualTo("FORMAT_VALID");
        assertThat(res.normalizedPayload().get("studentIdentifier")).isEqualTo("STU-2024-001");
    }

    @Test
    @DisplayName("Invalid APAAR format yields row errors and FORMAT_INVALID status")
    void testInvalidApaarFormat() {
        Map<String, String> row = Map.of(
                "studentIdentifier", "STU-2024-002",
                "fullName", "Priya Nair",
                "apaarId", "12345", // Only 5 digits
                "admissionCategory", "OBC"
        );

        ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                importJobId, 2, row, institutionId, academicYearId, programmeId, new HashSet<>(), new HashSet<>()
        );

        assertThat(res.status()).isEqualTo("INVALID");
        assertThat(res.errors()).anyMatch(e -> e.getErrorCode().equals("INVALID_APAAR_FORMAT"));
        assertThat(res.normalizedPayload().get("apaarFormatValid")).isEqualTo(false);
        assertThat(res.normalizedPayload().get("apaarStatus")).isEqualTo("FORMAT_INVALID");
    }

    @Test
    @DisplayName("Missing mandatory fields produces MISSING_MANDATORY errors")
    void testMissingMandatoryFields() {
        Map<String, String> row = Map.of(
                "apaarId", "123456789012"
                // Missing studentIdentifier, fullName, admissionCategory
        );

        ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                importJobId, 3, row, institutionId, academicYearId, programmeId, new HashSet<>(), new HashSet<>()
        );

        assertThat(res.status()).isEqualTo("INVALID");
        assertThat(res.errors()).hasSize(3);
        assertThat(res.errors()).allMatch(e -> e.getErrorCode().equals("MISSING_MANDATORY"));
    }

    @Test
    @DisplayName("Detects in-file duplicates for student identifiers and APAAR IDs")
    void testInFileDuplicates() {
        Map<String, String> row1 = Map.of(
                "studentIdentifier", "STU-DUP-01",
                "fullName", "Ananya Verma",
                "apaarId", "987654321098",
                "admissionCategory", "SC"
        );
        Map<String, String> row2 = Map.of(
                "studentIdentifier", "STU-DUP-01",
                "fullName", "Ananya Verma",
                "apaarId", "987654321098",
                "admissionCategory", "SC"
        );

        Set<String> seenIdentifiers = new HashSet<>();
        Set<String> seenApaar = new HashSet<>();

        ValidationEngine.RowValidationResult res1 = validationEngine.validateRow(
                importJobId, 1, row1, institutionId, academicYearId, programmeId, seenIdentifiers, seenApaar
        );
        assertThat(res1.status()).isEqualTo("VALID");

        ValidationEngine.RowValidationResult res2 = validationEngine.validateRow(
                importJobId, 2, row2, institutionId, academicYearId, programmeId, seenIdentifiers, seenApaar
        );
        assertThat(res2.status()).isEqualTo("DUPLICATE");
        assertThat(res2.errors()).anyMatch(e -> e.getErrorCode().equals("DUPLICATE_IN_FILE"));
    }

    @Test
    @DisplayName("Rejects admission categories that are not permitted by the schema")
    void testInvalidCategory() {
        Map<String, String> row = Map.of(
                "studentIdentifier", "STU-CAT-01",
                "fullName", "Category Test",
                "admissionCategory", "GOVERNMENT"
        );

        ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                importJobId, 1, row, institutionId, academicYearId, programmeId, new HashSet<>(), new HashSet<>()
        );

        assertThat(res.status()).isEqualTo("INVALID");
        assertThat(res.errors()).anyMatch(e -> e.getErrorCode().equals("INVALID_CATEGORY"));
    }

    @Test
    @DisplayName("Unrecognized gender values are stored as a schema-safe value")
    void testUnknownGenderNormalized() {
        Map<String, String> row = Map.of(
                "studentIdentifier", "STU-GENDER-01",
                "fullName", "Gender Test",
                "gender", "X",
                "admissionCategory", "OPEN"
        );

        ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                importJobId, 1, row, institutionId, academicYearId, programmeId, new HashSet<>(), new HashSet<>()
        );

        assertThat(res.status()).isEqualTo("VALID");
        assertThat(res.normalizedPayload().get("gender")).isEqualTo("PREFER_NOT_TO_SAY");
    }

    @Test
    @DisplayName("Parses multiple date formats like dd/MM/yyyy and yyyy-MM-dd")
    void testDateParsing() {
        Map<String, String> row = Map.of(
                "studentIdentifier", "STU-DATE-01",
                "fullName", "Karan Johar",
                "dateOfBirth", "25/12/2002",
                "admissionDate", "01-08-2024",
                "admissionCategory", "OPEN"
        );

        ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                importJobId, 1, row, institutionId, academicYearId, programmeId, new HashSet<>(), new HashSet<>()
        );

        assertThat(res.status()).isEqualTo("VALID");
        assertThat(res.normalizedPayload().get("dateOfBirth")).isEqualTo("2002-12-25");
        assertThat(res.normalizedPayload().get("admissionDate")).isEqualTo("2024-08-01");
    }
}

