package com.apaar.platform.student;

import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.student.dto.StudentDtos.StudentDetailDto;
import com.apaar.platform.student.dto.StudentDtos.StudentSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student identity registry and academic profiles")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @Operation(summary = "List registered students with pagination and search")
    public PageResponse<StudentSummaryDto> listStudents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search
    ) {
        return studentService.listStudents(page, size, search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @Operation(summary = "Get detailed student profile by ID")
    public StudentDetailDto getStudent(@PathVariable("id") UUID id) {
        return studentService.getStudent(id);
    }
}

