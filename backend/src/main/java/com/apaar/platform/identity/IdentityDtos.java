package com.apaar.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IdentityDtos {
    private IdentityDtos() {
    }

    public record ConflictDto(UUID id, String conflictType, String status, String severity, String reasonCode,
                              String detectionSource, UUID primaryStudentId, String primaryStudentName,
                              UUID secondaryStudentId, String secondaryStudentName,
                              UUID primaryEnrolmentId, UUID secondaryEnrolmentId, UUID assignedTo,
                              Map<String, Object> details, Instant createdAt, Instant resolvedAt) {
    }

    public record UpdateConflictRequest(
            @NotBlank @Pattern(regexp = "^(INVESTIGATING|RESOLVED|DISMISSED|OPEN)$") String status,
            @Size(max = 2000) String resolutionNotes) {
    }

    // Format validation
    public record ValidateApaarRequest(String apaarId, UUID studentId) {
    }

    public record FormatValidationResponse(boolean valid, String apaarId, String reasonCode, String message,
                                          String status, Instant validatedAt) {
    }

    // Verification
    public record VerificationRequestDto(UUID studentId, String mode) {
    }

    public record VerificationRecordDto(UUID id, UUID studentId, String studentName, String apaarId, String status,
                                        String verificationMode, String adapterName, String providerReference,
                                        String reasonCode, boolean simulated, boolean authoritative, String message,
                                        Instant verifiedAt, Instant requestedAt) {
    }

    // Matching
    public record MatchingRunRequest(UUID academicYearId, UUID institutionId, String runType) {
    }

    public record MatchingRunDto(UUID id, String runType, String status, int recordsScanned, int matchesFound,
                                 int conflictsGenerated, int conflictsSuppressed, UUID runBy, Instant startedAt,
                                 Instant completedAt, Map<String, Object> summary) {
    }

    public record IdentityMatchDto(UUID id, UUID runId, String matchType, String matchingRule,
                                   UUID primaryStudentId, String primaryStudentName, UUID primaryEnrolmentId,
                                   String primaryInstitutionName, UUID secondaryStudentId,
                                   String secondaryStudentName, UUID secondaryEnrolmentId,
                                   String secondaryInstitutionName, BigDecimal confidenceScore,
                                   String matchClassification, boolean reviewRequired,
                                   List<String> matchingFields, List<String> conflictingFields,
                                   Map<String, Object> details, UUID conflictId, Instant createdAt) {
    }

    // Conflict Evidence dossier
    public record ConflictEvidenceDto(UUID conflictId, String conflictType, String matchingRule,
                                      BigDecimal confidenceScore, Map<String, Object> primaryRecord,
                                      Map<String, Object> secondaryRecord, List<String> matchingFields,
                                      List<String> conflictingFields, List<VerificationRecordDto> verificationHistory,
                                      String explanation) {
    }

    // Identity Resolution Dashboard
    public record IdentityDashboardDto(long totalIdentitiesAssessed, long formatValidCount,
                                       long missingOrMalformedCount, long verificationPendingCount,
                                       long authoritativelyVerifiedCount, long simulatedVerifiedCount,
                                       long potentialDuplicatesCount, long openConflictsCount,
                                       long casesAwaitingReviewCount, long resolvedCasesCount) {
    }
}
