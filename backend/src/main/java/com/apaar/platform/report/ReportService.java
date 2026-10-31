package com.apaar.platform.report;

import com.apaar.platform.academic.AcademicYear;
import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.Programme;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ReportService {

    private final EnrolmentRepository enrolmentRepository;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final ProgrammeRepository programmeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AccessScopeService accessScope;

    public ReportService(EnrolmentRepository enrolmentRepository,
                         StudentRepository studentRepository,
                         InstitutionRepository institutionRepository,
                         ProgrammeRepository programmeRepository,
                         AcademicYearRepository academicYearRepository,
                         AccessScopeService accessScope) {
        this.enrolmentRepository = enrolmentRepository;
        this.studentRepository = studentRepository;
        this.institutionRepository = institutionRepository;
        this.programmeRepository = programmeRepository;
        this.academicYearRepository = academicYearRepository;
        this.accessScope = accessScope;
    }

    @Transactional(readOnly = true)
    public byte[] exportEnrolmentsCsv(UUID requestedInstitutionId, UUID academicYearId) {
        UUID effectiveInstitutionId = accessScope.resolveInstitutionFilter(requestedInstitutionId);

        List<Enrolment> enrolments;
        if (effectiveInstitutionId != null && academicYearId != null) {
            enrolments = enrolmentRepository.findByInstitutionIdAndAcademicYearId(effectiveInstitutionId, academicYearId);
        } else if (effectiveInstitutionId != null) {
            enrolments = enrolmentRepository.findAll().stream()
                    .filter(e -> effectiveInstitutionId.equals(e.getInstitutionId()))
                    .toList();
        } else if (academicYearId != null) {
            enrolments = enrolmentRepository.findAll().stream()
                    .filter(e -> academicYearId.equals(e.getAcademicYearId()))
                    .toList();
        } else {
            enrolments = enrolmentRepository.findAll();
        }

        StringWriter sw = new StringWriter();
        sw.write("Student ID,APAAR ID,Student Name,Date of Birth,Gender,Category,Institution Code,Institution Name,Programme Code,Programme Name,Academic Year,Enrolment Number,Admission Category,Status,Admission Date\r\n");

        Map<UUID, Student> studentMap = new HashMap<>();
        Map<UUID, Institution> instMap = new HashMap<>();
        Map<UUID, Programme> progMap = new HashMap<>();
        Map<UUID, AcademicYear> yearMap = new HashMap<>();

        for (Enrolment e : enrolments) {
            Student s = e.getStudentId() != null
                    ? studentMap.computeIfAbsent(e.getStudentId(), id -> studentRepository.findById(id).orElse(null))
                    : null;
            Institution inst = e.getInstitutionId() != null
                    ? instMap.computeIfAbsent(e.getInstitutionId(), id -> institutionRepository.findById(id).orElse(null))
                    : null;
            Programme p = e.getProgrammeId() != null
                    ? progMap.computeIfAbsent(e.getProgrammeId(), id -> programmeRepository.findById(id).orElse(null))
                    : null;
            AcademicYear ay = e.getAcademicYearId() != null
                    ? yearMap.computeIfAbsent(e.getAcademicYearId(), id -> academicYearRepository.findById(id).orElse(null))
                    : null;

            sw.write(csvCell(s != null && s.getId() != null ? s.getId().toString() : ""));
            sw.write(",");
            sw.write(csvCell(s != null ? s.getApaarId() : ""));
            sw.write(",");
            sw.write(csvCell(s != null ? s.getFullName() : ""));
            sw.write(",");
            sw.write(csvCell(s != null && s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : ""));
            sw.write(",");
            sw.write(csvCell(s != null ? s.getGender() : ""));
            sw.write(",");
            sw.write(csvCell(s != null ? s.getCategory() : ""));
            sw.write(",");
            sw.write(csvCell(inst != null ? inst.getCode() : ""));
            sw.write(",");
            sw.write(csvCell(inst != null ? inst.getName() : ""));
            sw.write(",");
            sw.write(csvCell(p != null ? p.getCode() : ""));
            sw.write(",");
            sw.write(csvCell(p != null ? p.getName() : ""));
            sw.write(",");
            sw.write(csvCell(ay != null ? ay.getCode() : ""));
            sw.write(",");
            sw.write(csvCell(e.getEnrolmentNumber()));
            sw.write(",");
            sw.write(csvCell(e.getAdmissionCategory()));
            sw.write(",");
            sw.write(csvCell(e.getStatus()));
            sw.write(",");
            sw.write(csvCell(e.getAdmissionDate() != null ? e.getAdmissionDate().toString() : ""));
            sw.write("\r\n");
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCell(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

