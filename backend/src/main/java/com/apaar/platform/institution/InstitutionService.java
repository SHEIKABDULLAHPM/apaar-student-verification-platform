package com.apaar.platform.institution;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ConflictException;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class InstitutionService {
    private final InstitutionRepository repository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;

    public InstitutionService(InstitutionRepository repository, AccessScopeService accessScope, AuditService auditService) {
        this.repository = repository;
        this.accessScope = accessScope;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<InstitutionDtos.InstitutionDto> list(String search, int page, int size) {
        UUID filter = accessScope.resolveInstitutionFilter(null);
        Specification<Institution> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (filter != null) predicate = cb.and(predicate, cb.equal(root.get("id"), filter));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like), cb.like(cb.lower(root.get("state")), like)));
            }
            return predicate;
        };
        Page<Institution> result = repository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        return PageResponse.from(result, InstitutionDtos::from);
    }

    @Transactional(readOnly = true)
    public InstitutionDtos.InstitutionDto get(UUID id) {
        accessScope.assertInstitutionAccess(id);
        return InstitutionDtos.from(require(id));
    }

    public Institution require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution", id));
    }

    @Transactional
    public InstitutionDtos.InstitutionDto create(InstitutionDtos.CreateInstitutionRequest request, UserPrincipal actor) {
        if (repository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new ConflictException("INSTITUTION_CODE_EXISTS", "Institution code already exists");
        }
        Institution institution = new Institution();
        institution.setCode(request.code().trim().toUpperCase());
        institution.setName(request.name().trim());
        institution.setAicteApprovalRef(request.aicteApprovalRef());
        institution.setInstitutionType(request.institutionType() == null ? "AFFILIATED" : request.institutionType());
        institution.setAddressLine(request.addressLine());
        institution.setCity(request.city());
        institution.setDistrict(request.district());
        institution.setState(request.state());
        institution.setPincode(request.pincode());
        institution.setContactEmail(request.contactEmail());
        institution.setContactPhone(request.contactPhone());
        institution.setCreatedBy(actor.getUserId());
        institution = repository.save(institution);
        auditService.record("INSTITUTION_CREATED", "INSTITUTION", institution.getId().toString(), "CREATE", "SUCCESS",
                institution.getId(), Map.of("code", institution.getCode()));
        return InstitutionDtos.from(institution);
    }

    @Transactional
    public InstitutionDtos.InstitutionDto update(UUID id, InstitutionDtos.UpdateInstitutionRequest request) {
        accessScope.assertInstitutionAccess(id);
        Institution institution = require(id);
        if (request.name() != null) institution.setName(request.name().trim());
        if (request.aicteApprovalRef() != null) institution.setAicteApprovalRef(request.aicteApprovalRef());
        if (request.institutionType() != null) institution.setInstitutionType(request.institutionType());
        if (request.addressLine() != null) institution.setAddressLine(request.addressLine());
        if (request.city() != null) institution.setCity(request.city());
        if (request.district() != null) institution.setDistrict(request.district());
        if (request.state() != null) institution.setState(request.state());
        if (request.pincode() != null) institution.setPincode(request.pincode());
        if (request.contactEmail() != null) institution.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) institution.setContactPhone(request.contactPhone());
        if (request.status() != null) institution.setStatus(request.status());
        institution = repository.save(institution);
        auditService.record("INSTITUTION_UPDATED", "INSTITUTION", id.toString(), "UPDATE", "SUCCESS", id, Map.of());
        return InstitutionDtos.from(institution);
    }
}
