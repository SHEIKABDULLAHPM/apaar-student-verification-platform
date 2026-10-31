package com.apaar.platform.cases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CaseDtos {

    public record CaseSummaryDto(
            UUID id,
            String caseNumber,
            String title,
            String subject,
            String status,
            String priority,
            String institutionName,
            String assignedTo,
            Instant openedAt,
            Instant createdAt
    ) {}

    public record CaseEvidenceDto(
            UUID id,
            String type,
            String description,
            String fileRef,
            Instant createdAt
    ) {}

    public record CaseCommentDto(
            UUID id,
            String authorName,
            String authorEmail,
            String body,
            Instant createdAt
    ) {}

    public record CaseDecisionDto(
            UUID id,
            String decision,
            String reason,
            String decidedBy,
            Instant createdAt
    ) {}

    public record CaseDetailDto(
            UUID id,
            String caseNumber,
            String title,
            String subject,
            String status,
            String priority,
            String institutionName,
            String assignedTo,
            Instant openedAt,
            Instant createdAt,
            String description,
            List<CaseEvidenceDto> evidence,
            List<CaseCommentDto> comments,
            List<CaseDecisionDto> decisions
    ) {}

    public record AddCommentRequest(
            @NotBlank(message = "Comment body is required")
            String body
    ) {}

    public record CreateCaseRequest(
            @NotBlank(message = "Title is required")
            String title,
            String caseType,
            String description,
            String priority,
            UUID institutionId
    ) {}

    public record RecordDecisionRequest(
            @NotBlank(message = "Decision code is required")
            @Pattern(regexp = "NO_ACTION|CORRECTION_APPROVED|ENROLMENT_CANCELLED|REFERRED|DUPLICATE_CONFIRMED|DATA_ERROR|IDENTITY_CONFIRMED|DUPLICATE_RECORD_CONFIRMED|ENROLMENT_CONFLICT_CONFIRMED|DATA_ENTRY_ERROR|LEGITIMATE_HISTORICAL_ENROLMENT|TRANSFER_OR_STATUS_CHANGE_CONFIRMED|NO_ACTION_REQUIRED|INSUFFICIENT_EVIDENCE|REFERRED_FOR_FURTHER_REVIEW",
                    message = "Unsupported decision code")
            String decisionCode,
            @NotBlank(message = "Rationale is required")
            String rationale,
            String effectiveAction,
            UUID studentIdToCorrect,
            String correctedFullName,
            String correctedDateOfBirth,
            String correctedGender
    ) {}

    public record UpdateCaseStatusRequest(
            @NotBlank(message = "Status is required")
            String status,
            String notes
    ) {}

    public record AssignCaseRequest(
            UUID assignedTo
    ) {}

    public record AddEvidenceRequest(
            @NotBlank(message = "Evidence type is required")
            String type,
            @NotBlank(message = "Description is required")
            String description,
            String storageRef
    ) {}
}

