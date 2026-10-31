package com.apaar.platform.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {
    @Query("""
            select a from AuditEvent a
            where (:institutionId is null or a.institutionId = :institutionId)
            order by a.occurredAt desc
            """)
    Page<AuditEvent> search(@Param("institutionId") UUID institutionId, Pageable pageable);
}
