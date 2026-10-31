package com.apaar.platform.academic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AcademicDtos {
    private AcademicDtos() {
    }

    public record AcademicYearDto(UUID id, String code, LocalDate startDate, LocalDate endDate, boolean active) {
        static AcademicYearDto from(AcademicYear y) {
            return new AcademicYearDto(y.getId(), y.getCode(), y.getStartDate(), y.getEndDate(), y.isActive());
        }
    }

    public record CreateAcademicYearRequest(
            @NotBlank @Pattern(regexp = "^[0-9]{4}-[0-9]{2}$", message = "Use format YYYY-YY") String code,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            boolean active) {
    }

    public record ProgrammeDto(UUID id, UUID institutionId, String code, String name, String degreeLevel,
                               BigDecimal durationYears, String status, Integer approvedIntake, Instant createdAt) {
    }

    public record CreateProgrammeRequest(
            UUID institutionId,
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Pattern(regexp = "^(DIPLOMA|UG|PG|DOCTORAL)$") String degreeLevel,
            @NotNull @Min(1) BigDecimal durationYears) {
    }

    public record UpdateProgrammeRequest(
            @Size(max = 255) String name,
            @Pattern(regexp = "^(DIPLOMA|UG|PG|DOCTORAL)$") String degreeLevel,
            @Min(1) BigDecimal durationYears,
            @Pattern(regexp = "^(ACTIVE|SUSPENDED|DISCONTINUED)$") String status) {
    }

    public record CategorySeat(
            @NotBlank @Pattern(regexp = "^(GENERAL|OBC|SC|ST|EWS|NRI|OCI|FOREIGN_NATIONAL|SUPERNUMERARY)$") String category,
            @Min(0) int approvedSeats) {
    }

    public record UpsertIntakeRequest(
            @Min(0) int approvedIntake,
            @Min(0) int supernumeraryIntake,
            @Min(0) int nriIntake,
            @Min(0) int ociIntake,
            @Min(0) int foreignNationalIntake,
            @Valid List<CategorySeat> categories) {
    }

    public record IntakeDto(UUID id, UUID programmeId, UUID academicYearId, int approvedIntake,
                            int supernumeraryIntake, int nriIntake, int ociIntake, int foreignNationalIntake,
                            String status, List<CategorySeat> categories) {
    }
}
