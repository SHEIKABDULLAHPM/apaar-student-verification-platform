package com.apaar.platform.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRecordVersionRepository extends JpaRepository<StudentRecordVersion, UUID> {
    List<StudentRecordVersion> findByStudentIdOrderByVersionNoDesc(UUID studentId);

    @Query("SELECT coalesce(max(v.versionNo), 0) FROM StudentRecordVersion v WHERE v.studentId = :studentId")
    int findLatestVersionNo(@Param("studentId") UUID studentId);

    Optional<StudentRecordVersion> findByStudentIdAndVersionNo(UUID studentId, Integer versionNo);
}

