package com.apaar.platform.institution;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.security.AccessScopeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock
    private InstitutionRepository repository;

    @Mock
    private AccessScopeService accessScope;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InstitutionService service;

    @Test
    @DisplayName("Get institution asserts access scope and returns DTO")
    void getInstitutionSuccess() {
        UUID instId = UUID.randomUUID();
        Institution institution = new Institution();
        institution.setId(instId);
        institution.setCode("SIT-TEST");
        institution.setName("Sunrise Institute of Technology");
        institution.setAicteApprovalRef("AICTE/TEST/001");

        when(repository.findById(instId)).thenReturn(Optional.of(institution));

        InstitutionDtos.InstitutionDto dto = service.get(instId);

        verify(accessScope).assertInstitutionAccess(instId);
        assertThat(dto.code()).isEqualTo("SIT-TEST");
        assertThat(dto.name()).isEqualTo("Sunrise Institute of Technology");
    }

    @Test
    @DisplayName("Get non-existent institution throws ResourceNotFoundException")
    void getInstitutionNotFound() {
        UUID instId = UUID.randomUUID();
        when(repository.findById(instId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(instId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Institution");
    }
}
