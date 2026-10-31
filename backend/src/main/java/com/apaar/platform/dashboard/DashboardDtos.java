package com.apaar.platform.dashboard;

import java.util.List;

public class DashboardDtos {

    public record IntakeVsEnrolmentDto(
            String institutionName,
            String programmeName,
            String academicYear,
            int approvedIntake,
            int actualEnrolment
    ) {}

    public record CountByCategoryDto(
            String category,
            int count
    ) {}

    public record VerificationActivityDto(
            String date,
            int count
    ) {}

    public record DashboardSummaryDto(
            String generatedAt,
            String institutionId,
            String institutionName,
            Long institutionCount,
            int studentCount,
            int enrolmentCount,
            int apaarVerifiedCount,
            int apaarUnverifiedCount,
            int openConflictCount,
            int openCaseCount,
            int complianceFindingCount,
            int anomalyAlertCount,
            List<IntakeVsEnrolmentDto> intakeVsEnrolment,
            List<CountByCategoryDto> enrolmentsByCategory,
            List<VerificationActivityDto> verificationActivity
    ) {}
}

