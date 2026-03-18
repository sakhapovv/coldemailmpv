package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.sakhapov.emailwarmup.api.dto.CreateProspectRequest;
import ru.sakhapov.emailwarmup.api.dto.ImportProspectsResponse;
import ru.sakhapov.emailwarmup.api.dto.ProspectResponse;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProspectServiceImplTest {

    @Mock
    private ProspectRepository prospectRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private ProspectServiceImpl prospectService;

    @Test
    void createProspectShouldPersistNewProspect() {
        Workspace workspace = Workspace.builder().id(10L).build();
        CreateProspectRequest request = new CreateProspectRequest();
        request.setEmail("john@acme.com");
        request.setFirstName("John");
        request.setCompany("Acme");

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(prospectRepository.existsByWorkspaceIdAndEmailIgnoreCase(10L, "john@acme.com")).thenReturn(false);
        when(prospectRepository.save(any(Prospect.class))).thenAnswer(invocation -> {
            Prospect prospect = invocation.getArgument(0);
            prospect.setId(1L);
            return prospect;
        });

        ProspectResponse response = prospectService.createProspect("owner@test.com", request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("john@acme.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getCompany()).isEqualTo("Acme");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createProspectShouldRejectDuplicateEmail() {
        Workspace workspace = Workspace.builder().id(10L).build();
        CreateProspectRequest request = new CreateProspectRequest();
        request.setEmail("john@acme.com");

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(prospectRepository.existsByWorkspaceIdAndEmailIgnoreCase(10L, "john@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> prospectService.createProspect("owner@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prospect with this email already exists in workspace");
    }

    @Test
    void importProspectsShouldCreateNewRowsAndSkipDuplicates() {
        Workspace workspace = Workspace.builder().id(10L).build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "prospects.csv",
                "text/csv",
                """
                email,firstName,lastName,company,jobTitle,website
                john@acme.com,John,Doe,Acme,Founder,https://acme.com
                existing@beta.com,Jane,Smith,Beta,CEO,https://beta.com
                ,No,Email,Oops,Role,https://oops.com
                """.getBytes(StandardCharsets.UTF_8)
        );

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(prospectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(
                Prospect.builder().email("existing@beta.com").build()
        ));
        when(prospectRepository.save(any(Prospect.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportProspectsResponse response = prospectService.importProspects("owner@test.com", file);

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(2);
        assertThat(response.errors()).containsExactly("Row 4: email is required");
        verify(prospectRepository).save(any(Prospect.class));
    }

    @Test
    void importProspectsShouldRejectMissingEmailHeader() {
        Workspace workspace = Workspace.builder().id(10L).build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "prospects.csv",
                "text/csv",
                """
                firstName,lastName
                John,Doe
                """.getBytes(StandardCharsets.UTF_8)
        );

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(prospectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> prospectService.importProspects("owner@test.com", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV header must include email");
    }
}
