package com.apaar.platform.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, UUID>, JpaSpecificationExecutor<Institution> {
    Optional<Institution> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
