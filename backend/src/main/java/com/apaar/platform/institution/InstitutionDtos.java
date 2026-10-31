package com.apaar.platform.institution;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class InstitutionDtos {
    private InstitutionDtos() {
    }

    public record InstitutionDto(UUID id, String code, String name, String aicteApprovalRef, String institutionType,
                                 String addressLine, String city, String district, String state, String pincode,
                                 String contactEmail, String contactPhone, String status, Instant createdAt) {
    }

    public record CreateInstitutionRequest(
            @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Code may contain letters, digits and hyphens") String code,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 64) String aicteApprovalRef,
            @Pattern(regexp = "^(GOVERNMENT|PRIVATE|DEEMED|AFFILIATED|AUTONOMOUS)$") String institutionType,
            @Size(max = 255) String addressLine,
            @Size(max = 128) String city,
            @Size(max = 128) String district,
            @Size(max = 128) String state,
            @Pattern(regexp = "^$|^[0-9]{6}$", message = "Pincode must be 6 digits") String pincode,
            @Email @Size(max = 255) String contactEmail,
            @Size(max = 32) String contactPhone) {
    }

    public record UpdateInstitutionRequest(
            @Size(max = 255) String name,
            @Size(max = 64) String aicteApprovalRef,
            @Pattern(regexp = "^(GOVERNMENT|PRIVATE|DEEMED|AFFILIATED|AUTONOMOUS)$") String institutionType,
            @Size(max = 255) String addressLine,
            @Size(max = 128) String city,
            @Size(max = 128) String district,
            @Size(max = 128) String state,
            @Pattern(regexp = "^$|^[0-9]{6}$") String pincode,
            @Email @Size(max = 255) String contactEmail,
            @Size(max = 32) String contactPhone,
            @Pattern(regexp = "^(ACTIVE|SUSPENDED|INACTIVE)$") String status) {
    }

    static InstitutionDto from(Institution i) {
        return new InstitutionDto(i.getId(), i.getCode(), i.getName(), i.getAicteApprovalRef(), i.getInstitutionType(),
                i.getAddressLine(), i.getCity(), i.getDistrict(), i.getState(), i.getPincode(), i.getContactEmail(),
                i.getContactPhone(), i.getStatus(), i.getCreatedAt());
    }
}
