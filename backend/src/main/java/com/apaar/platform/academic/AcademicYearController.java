package com.apaar.platform.academic;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@Tag(name = "Academic years", description = "Academic year configuration")
public class AcademicYearController {
    private final AcademicYearService service;

    public AcademicYearController(AcademicYearService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACADEMIC_YEAR_READ')")
    @Operation(summary = "List academic years, newest first")
    public List<AcademicDtos.AcademicYearDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMIC_YEAR_WRITE')")
    @Operation(summary = "Create an academic year; activating one deactivates the previous active year")
    public AcademicDtos.AcademicYearDto create(@Valid @RequestBody AcademicDtos.CreateAcademicYearRequest request) {
        return service.create(request);
    }
}
