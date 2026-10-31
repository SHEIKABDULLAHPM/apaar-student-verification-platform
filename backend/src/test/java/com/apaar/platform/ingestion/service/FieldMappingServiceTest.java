package com.apaar.platform.ingestion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldMappingServiceTest {

    private FieldMappingService mappingService;

    @BeforeEach
    void setUp() {
        mappingService = new FieldMappingService();
    }

    @Test
    @DisplayName("Should correctly auto-detect column mappings for varied aliases")
    void testSuggestMappings() {
        List<String> headers = List.of(
                "Roll No",
                "Student Name",
                "APAAR ID",
                "DOB",
                "Sex",
                "Category",
                "Admission Date",
                "Student Email",
                "Contact Phone"
        );

        Map<String, String> suggested = mappingService.suggestMappings(headers);

        assertThat(suggested.get("Roll No")).isEqualTo(FieldMappingService.FIELD_STUDENT_IDENTIFIER);
        assertThat(suggested.get("Student Name")).isEqualTo(FieldMappingService.FIELD_FULL_NAME);
        assertThat(suggested.get("APAAR ID")).isEqualTo(FieldMappingService.FIELD_APAAR_ID);
        assertThat(suggested.get("DOB")).isEqualTo(FieldMappingService.FIELD_DATE_OF_BIRTH);
        assertThat(suggested.get("Sex")).isEqualTo(FieldMappingService.FIELD_GENDER);
        assertThat(suggested.get("Category")).isEqualTo(FieldMappingService.FIELD_ADMISSION_CATEGORY);
        assertThat(suggested.get("Admission Date")).isEqualTo(FieldMappingService.FIELD_ADMISSION_DATE);
        assertThat(suggested.get("Student Email")).isEqualTo(FieldMappingService.FIELD_EMAIL);
        assertThat(suggested.get("Contact Phone")).isEqualTo(FieldMappingService.FIELD_PHONE);
    }

    @Test
    @DisplayName("Should detect missing mandatory fields")
    void testMissingMandatoryFields() {
        Map<String, String> partialMappings = Map.of(
                "ColA", FieldMappingService.FIELD_STUDENT_IDENTIFIER
        );

        List<String> missing = mappingService.getMissingMandatoryFields(partialMappings);
        assertThat(missing).contains(
                FieldMappingService.FIELD_FULL_NAME,
                FieldMappingService.FIELD_ADMISSION_CATEGORY
        );
    }

    @Test
    @DisplayName("Should map raw row correctly to canonical attributes")
    void testMapRowToCanonical() {
        Map<String, String> rawData = Map.of(
                "RegNo", "REG-101",
                "Full Name", "Aarav Sharma",
                "Category", "OPEN"
        );
        Map<String, String> mappings = Map.of(
                "RegNo", FieldMappingService.FIELD_STUDENT_IDENTIFIER,
                "Full Name", FieldMappingService.FIELD_FULL_NAME,
                "Category", FieldMappingService.FIELD_ADMISSION_CATEGORY
        );

        Map<String, String> canonical = mappingService.mapRowToCanonical(rawData, mappings);
        assertThat(canonical.get("studentIdentifier")).isEqualTo("REG-101");
        assertThat(canonical.get("fullName")).isEqualTo("Aarav Sharma");
        assertThat(canonical.get("admissionCategory")).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Should detect conflicting multiple columns mapped to same target canonical field")
    void testConflictingMappings() {
        Map<String, String> mappings = Map.of(
                "Col1", FieldMappingService.FIELD_STUDENT_IDENTIFIER,
                "Col2", FieldMappingService.FIELD_STUDENT_IDENTIFIER
        );

        List<String> conflicts = mappingService.findConflictingMappings(mappings);
        assertThat(conflicts).isNotEmpty();
    }
}

