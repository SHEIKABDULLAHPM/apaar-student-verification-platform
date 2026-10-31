package com.apaar.platform.student.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StudentDtos {

    public record StudentSummaryDto(
            UUID id,
            String apaarId,
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String category,
            Boolean apaarFormatValid,
            String apaarVerificationStatus,
            Instant createdAt
    ) {}

    public record StudentEnrolmentDto(
            UUID id,
            UUID institutionId,
            String institutionName,
            String programmeName,
            String academicYear,
            String enrolmentNumber,
            String admissionCategory,
            String status
    ) {}

    public record StudentDetailDto(
            UUID id,
            String apaarId,
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String category,
            Boolean apaarFormatValid,
            String apaarVerificationStatus,
            Instant createdAt,
            List<StudentEnrolmentDto> enrolments
    ) {}

    public record EnrolmentDto(
            UUID id,
            UUID studentId,
            String studentName,
            UUID institutionId,
            String institutionName,
            UUID programmeId,
            String programmeName,
            String academicYear,
            UUID academicYearId,
            String enrolmentNumber,
            String admissionCategory,
            LocalDate admissionDate,
            String status
    ) {}
}

