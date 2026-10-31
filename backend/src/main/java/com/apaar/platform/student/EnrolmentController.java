package com.apaar.platform.student;

import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.student.dto.StudentDtos.EnrolmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrolments")
@Tag(name = "Enrolments", description = "Student academic enrolments and institutional allocations")
public class EnrolmentController {

    private final EnrolmentService enrolmentService;

    public EnrolmentController(EnrolmentService enrolmentService) {
        this.enrolmentService = enrolmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ENROLMENT_READ')")
    @Operation(summary = "List student enrolments with filtering and pagination")
    public PageResponse<EnrolmentDto> listEnrolments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "institutionId", required = false) UUID institutionId,
            @RequestParam(value = "academicYearId", required = false) UUID academicYearId,
            @RequestParam(value = "programmeId", required = false) UUID programmeId,
            @RequestParam(value = "status", required = false) String status
    ) {
        return enrolmentService.listEnrolments(page, size, institutionId, academicYearId, programmeId, status);
    }
}

