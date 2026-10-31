package com.apaar.platform.institution;

import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/institutions")
@Tag(name = "Institutions", description = "Institution registration and profile management")
public class InstitutionController {
    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INSTITUTION_READ')")
    @Operation(summary = "List institutions visible to the current role")
    public PageResponse<InstitutionDtos.InstitutionDto> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INSTITUTION_READ')")
    public InstitutionDtos.InstitutionDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Register an AICTE-approved institution (platform administrator)")
    public InstitutionDtos.InstitutionDto create(@Valid @RequestBody InstitutionDtos.CreateInstitutionRequest request,
                                                 @AuthenticationPrincipal UserPrincipal actor) {
        return service.create(request, actor);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('INSTITUTION_WRITE')")
    @Operation(summary = "Update an institution profile")
    public InstitutionDtos.InstitutionDto update(@PathVariable UUID id,
                                                 @Valid @RequestBody InstitutionDtos.UpdateInstitutionRequest request) {
        return service.update(id, request);
    }
}
