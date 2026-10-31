package com.apaar.platform.ingestion.service;

import com.apaar.platform.apaar.ApaarFormatValidator;
import com.apaar.platform.ingestion.dto.ImportDtos;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.StudentRecordVersionRepository;
import com.apaar.platform.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apaar.platform.student.Student;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private StudentRecordVersionRepository recordVersionRepository;

    private SubmissionService service;
    private final UUID institutionId = UUID.randomUUID();
    private final UUID academicYearId = UUID.randomUUID();
    private final UUID programmeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubmissionService(new ApaarFormatValidator(), studentRepository,
                enrolmentRepository, recordVersionRepository);
    }

    @Test
    @DisplayName("REST submission rejects invalid email, phone and implausible dates")
    void testInvalidFormatsRejected() {
        ImportDtos.SingleSubmissionRequest request = new ImportDtos.SingleSubmissionRequest(
                institutionId,
                academicYearId,
                programmeId,
                "STU-API-001",
                "900000000201",
                "API Test Student",
                LocalDate.now().minusYears(120),
                "FEMALE",
                "OPEN",
                LocalDate.now().plusYears(2),
                "not-an-email",
                "abcdefghij",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ImportDtos.SingleSubmissionResult result = service.submitSingle(request, institutionId, userId);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).extracting(ImportDtos.RowErrorDto::errorCode)
                .contains("INVALID_EMAIL", "INVALID_PHONE", "IMPLAUSIBLE_DATE");
        verifyNoInteractions(studentRepository, enrolmentRepository, recordVersionRepository);
    }

    @Test
    @DisplayName("REST submission trims the identifier and APAAR ID and normalizes contact fields")
    void testNormalization() {
        ImportDtos.SingleSubmissionRequest request = new ImportDtos.SingleSubmissionRequest(
                institutionId,
                academicYearId,
                programmeId,
                "  STU-API-NORM  ",
                " 900000000298 ",
                "API Normalization Student",
                LocalDate.of(2005, 1, 1),
                "FEMALE",
                "OPEN",
                LocalDate.now().minusMonths(1),
                "  Student@Example.EDU.in ",
                "+91 (987) 654-3210",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(studentRepository.findByApaarId("900000000298")).thenReturn(Optional.empty());
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrolmentRepository.findByInstitutionIdAndEnrolmentNumber(institutionId, "STU-API-NORM"))
                .thenReturn(Optional.empty());
        when(enrolmentRepository.findByStudentIdAndAcademicYearIdAndStatusIn(any(), eq(academicYearId), anyList()))
                .thenReturn(List.of());
        when(enrolmentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recordVersionRepository.findLatestVersionNo(any())).thenReturn(0);
        when(recordVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportDtos.SingleSubmissionResult result = service.submitSingle(request, institutionId, userId);

        assertThat(result.success()).isTrue();
        assertThat(result.studentIdentifier()).isEqualTo("STU-API-NORM");

        org.mockito.ArgumentCaptor<Student> studentCaptor = org.mockito.ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).saveAndFlush(studentCaptor.capture());
        Student saved = studentCaptor.getValue();
        assertThat(saved.getApaarId()).isEqualTo("900000000298");
        assertThat(saved.getEmail()).isEqualTo("student@example.edu.in");
        assertThat(saved.getPhone()).isEqualTo("919876543210");
    }

    @Test
    @DisplayName("REST submission requires an institution when the caller is platform-wide")
    void testMissingInstitutionRejected() {
        ImportDtos.SingleSubmissionRequest request = new ImportDtos.SingleSubmissionRequest(
                null,
                academicYearId,
                programmeId,
                "STU-API-002",
                null,
                "API Test Student",
                LocalDate.of(2004, 1, 1),
                "MALE",
                "OPEN",
                LocalDate.now().minusMonths(1),
                "student@example.edu.in",
                "9876543210",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ImportDtos.SingleSubmissionResult result = service.submitSingle(request, null, userId);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).extracting(ImportDtos.RowErrorDto::errorCode)
                .contains("INSTITUTION_REQUIRED");
        verifyNoInteractions(studentRepository, enrolmentRepository, recordVersionRepository);
    }
}
