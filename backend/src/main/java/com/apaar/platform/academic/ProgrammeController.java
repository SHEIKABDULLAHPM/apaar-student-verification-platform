package com.apaar.platform.academic;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/programmes")
@Tag(name = "Programmes and intake", description = "Programme catalogue and approved intake configuration")
public class ProgrammeController {
    private final ProgrammeService service;

    public ProgrammeController(ProgrammeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROGRAMME_READ')")
    public PageResponse<AcademicDtos.ProgrammeDto> list(
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(institutionId, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROGRAMME_READ')")
    public AcademicDtos.ProgrammeDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROGRAMME_WRITE')")
    @Operation(summary = "Create a programme for an institution")
    public AcademicDtos.ProgrammeDto create(@Valid @RequestBody AcademicDtos.CreateProgrammeRequest request,
                                           @AuthenticationPrincipal UserPrincipal actor) {
        return service.create(request, actor);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('PROGRAMME_WRITE')")
    public AcademicDtos.ProgrammeDto update(@PathVariable UUID id,
                                            @Valid @RequestBody AcademicDtos.UpdateProgrammeRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/intakes")
    @PreAuthorize("hasAuthority('PROGRAMME_READ')")
    @Operation(summary = "List approved-intake configuration for a programme")
    public List<AcademicDtos.IntakeDto> intakes(@PathVariable UUID id) {
        return service.listIntakes(id);
    }

    @PutMapping("/{id}/intakes/{academicYearId}")
    @PreAuthorize("hasAuthority('PROGRAMME_WRITE')")
    @Operation(summary = "Create or replace approved-intake configuration including category seats")
    public AcademicDtos.IntakeDto upsertIntake(@PathVariable UUID id, @PathVariable UUID academicYearId,
                                               @Valid @RequestBody AcademicDtos.UpsertIntakeRequest request,
                                               @AuthenticationPrincipal UserPrincipal actor) {
        return service.upsertIntake(id, academicYearId, request, actor);
    }
}
