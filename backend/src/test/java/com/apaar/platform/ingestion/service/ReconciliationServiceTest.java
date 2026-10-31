package com.apaar.platform.ingestion.service;

import com.apaar.platform.ingestion.DataImportJob;
import com.apaar.platform.ingestion.DataImportRow;
import com.apaar.platform.ingestion.DataImportRowRepository;
import com.apaar.platform.ingestion.dto.ImportDtos.ReconciliationSummaryDto;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DataImportRowRepository rowRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    @DisplayName("Correctly reconciles new admissions, updates, unchanged, and missing records")
    void testComputeReconciliation() {
        UUID jobId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID progId = UUID.randomUUID();

        DataImportJob job = new DataImportJob();
        job.setId(jobId);
        job.setInstitutionId(instId);
        job.setAcademicYearId(yearId);
        job.setProgrammeId(progId);

        // Previous enrolment 1: student STU-1 (will be updated)
        UUID student1Id = UUID.randomUUID();
        Enrolment enr1 = new Enrolment();
        enr1.setStudentId(student1Id);
        enr1.setEnrolmentNumber("STU-1");
        enr1.setAdmissionCategory("OPEN");
        enr1.setStatus("ACTIVE");
        enr1.setProgrammeId(progId);

        Student s1 = new Student();
        s1.setFullName("Student One");
        s1.setApaarId("111111111111");
        s1.setEmail("old@example.com");

        // Previous enrolment 2: student STU-2 (will be missing from snapshot)
        UUID student2Id = UUID.randomUUID();
        Enrolment enr2 = new Enrolment();
        enr2.setStudentId(student2Id);
        enr2.setEnrolmentNumber("STU-2");
        enr2.setAdmissionCategory("OBC");
        enr2.setStatus("ACTIVE");
        enr2.setProgrammeId(progId);

        Student s2 = new Student();
        s2.setFullName("Student Two");

        when(enrolmentRepository.findByInstitutionIdAndAcademicYearId(instId, yearId))
                .thenReturn(List.of(enr1, enr2));
        when(studentRepository.findById(student1Id)).thenReturn(Optional.of(s1));
        when(studentRepository.findById(student2Id)).thenReturn(Optional.of(s2));

        // Rows in current import:
        // Row 1: STU-1 with updated email
        DataImportRow r1 = new DataImportRow(jobId, 1, Map.of());
        r1.setRowStatus("VALID");
        r1.setNormalizedPayload(Map.of(
                "studentIdentifier", "STU-1",
                "fullName", "Student One",
                "apaarId", "111111111111",
                "email", "new@example.com"
        ));

        // Row 2: STU-3 brand new admission
        DataImportRow r2 = new DataImportRow(jobId, 2, Map.of());
        r2.setRowStatus("VALID");
        r2.setNormalizedPayload(Map.of(
                "studentIdentifier", "STU-3",
                "fullName", "Student Three",
                "apaarId", "333333333333",
                "email", "three@example.com"
        ));

        when(rowRepository.findByImportJobIdOrderByRowNumberAsc(jobId)).thenReturn(List.of(r1, r2));

        ReconciliationSummaryDto summary = reconciliationService.computeReconciliation(job);

        assertThat(summary.totalCurrentSubmission()).isEqualTo(2);
        assertThat(summary.newAdmissions()).isEqualTo(1);
        assertThat(summary.updatedCount()).isEqualTo(1);
        assertThat(summary.unchangedCount()).isEqualTo(0);
        assertThat(summary.missingFromSnapshotCount()).isEqualTo(1);

        assertThat(summary.items()).anyMatch(i -> i.studentIdentifier().equals("STU-3") && i.action().equals("NEW_ADMISSION"));
        assertThat(summary.items()).anyMatch(i -> i.studentIdentifier().equals("STU-1") && i.action().equals("DATA_UPDATE"));
        assertThat(summary.items()).anyMatch(i -> i.studentIdentifier().equals("STU-2") && i.action().equals("MISSING_FROM_SNAPSHOT"));
    }

    @Test
    @DisplayName("Name and admission category changes are reported as a data update")
    void testNameAndCategoryChangesReported() {
        UUID jobId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID progId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        DataImportJob job = new DataImportJob();
        job.setId(jobId);
        job.setInstitutionId(instId);
        job.setAcademicYearId(yearId);
        job.setProgrammeId(progId);

        Enrolment enrolment = new Enrolment();
        enrolment.setStudentId(studentId);
        enrolment.setEnrolmentNumber("STU-C");
        enrolment.setAdmissionCategory("OPEN");
        enrolment.setStatus("ACTIVE");
        enrolment.setProgrammeId(progId);

        Student student = new Student();
        student.setFullName("Old Name");
        student.setCategory("OPEN");

        when(enrolmentRepository.findByInstitutionIdAndAcademicYearId(instId, yearId))
                .thenReturn(List.of(enrolment));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        DataImportRow row = new DataImportRow(jobId, 1, Map.of());
        row.setRowStatus("VALID");
        row.setNormalizedPayload(Map.of(
                "studentIdentifier", "STU-C",
                "fullName", "Corrected Name",
                "admissionCategory", "OBC"
        ));
        when(rowRepository.findByImportJobIdOrderByRowNumberAsc(jobId)).thenReturn(List.of(row));

        ReconciliationSummaryDto summary = reconciliationService.computeReconciliation(job);

        assertThat(summary.updatedCount()).isEqualTo(1);
        assertThat(summary.items()).anyMatch(i -> i.studentIdentifier().equals("STU-C")
                && i.action().equals("DATA_UPDATE")
                && i.changedFields().containsKey("fullName")
                && i.changedFields().containsKey("admissionCategory"));
    }

    @Test
    @DisplayName("Withdrawn enrolment matched by commit is reported as an update, not a new admission")
    void testWithdrawnEnrolmentIncludedInLookup() {
        UUID jobId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID progId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        DataImportJob job = new DataImportJob();
        job.setId(jobId);
        job.setInstitutionId(instId);
        job.setAcademicYearId(yearId);
        job.setProgrammeId(progId);

        Enrolment withdrawn = new Enrolment();
        withdrawn.setStudentId(studentId);
        withdrawn.setEnrolmentNumber("STU-W");
        withdrawn.setAdmissionCategory("OPEN");
        withdrawn.setStatus("WITHDRAWN");
        withdrawn.setProgrammeId(progId);

        Student student = new Student();
        student.setFullName("Withdrawn Student");

        when(enrolmentRepository.findByInstitutionIdAndAcademicYearId(instId, yearId))
                .thenReturn(List.of(withdrawn));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        DataImportRow row = new DataImportRow(jobId, 1, Map.of());
        row.setRowStatus("VALID");
        row.setNormalizedPayload(Map.of(
                "studentIdentifier", "STU-W",
                "fullName", "Withdrawn Student"
        ));
        when(rowRepository.findByImportJobIdOrderByRowNumberAsc(jobId)).thenReturn(List.of(row));

        ReconciliationSummaryDto summary = reconciliationService.computeReconciliation(job);

        assertThat(summary.newAdmissions()).isEqualTo(0);
        assertThat(summary.unchangedCount()).isEqualTo(1);
        assertThat(summary.missingFromSnapshotCount()).isEqualTo(0);
        assertThat(summary.items()).anyMatch(i -> i.studentIdentifier().equals("STU-W")
                && i.action().equals("UNCHANGED"));
    }
}

