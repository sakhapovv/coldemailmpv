package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.CreateSuppressionRequest;
import ru.sakhapov.emailwarmup.api.dto.SuppressionEntryResponse;
import ru.sakhapov.emailwarmup.store.entity.SuppressionEntry;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.SuppressionEntryRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuppressionServiceImplTest {

    @Mock
    private SuppressionEntryRepository suppressionEntryRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private SuppressionServiceImpl suppressionService;

    @Test
    void createSuppressionShouldPersistNewEntry() {
        Workspace workspace = Workspace.builder().id(10L).build();
        CreateSuppressionRequest request = new CreateSuppressionRequest();
        request.setEmail("blocked@test.com");
        request.setReason(SuppressionReason.MANUAL);

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(suppressionEntryRepository.existsByWorkspaceIdAndEmailIgnoreCase(10L, "blocked@test.com")).thenReturn(false);
        when(suppressionEntryRepository.save(any(SuppressionEntry.class))).thenAnswer(invocation -> {
            SuppressionEntry entry = invocation.getArgument(0);
            entry.setId(1L);
            return entry;
        });

        SuppressionEntryResponse response = suppressionService.createSuppression("owner@test.com", request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("blocked@test.com");
        assertThat(response.getReason()).isEqualTo("MANUAL");
    }

    @Test
    void createSuppressionShouldRejectDuplicateEmail() {
        Workspace workspace = Workspace.builder().id(10L).build();
        CreateSuppressionRequest request = new CreateSuppressionRequest();
        request.setEmail("blocked@test.com");
        request.setReason(SuppressionReason.MANUAL);

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(suppressionEntryRepository.existsByWorkspaceIdAndEmailIgnoreCase(10L, "blocked@test.com")).thenReturn(true);

        assertThatThrownBy(() -> suppressionService.createSuppression("owner@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already suppressed in workspace");
    }

    @Test
    void listSuppressionsShouldReturnMappedEntries() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SuppressionEntry entry = SuppressionEntry.builder()
                .id(1L)
                .workspace(workspace)
                .email("blocked@test.com")
                .reason(SuppressionReason.BOUNCED)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(suppressionEntryRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(entry));

        List<SuppressionEntryResponse> responses = suppressionService.listSuppressions("owner@test.com");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getReason()).isEqualTo("BOUNCED");
    }

    @Test
    void deleteSuppressionShouldRemoveExistingEntry() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SuppressionEntry entry = SuppressionEntry.builder()
                .id(1L)
                .workspace(workspace)
                .email("blocked@test.com")
                .reason(SuppressionReason.MANUAL)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(suppressionEntryRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(entry));

        suppressionService.deleteSuppression("owner@test.com", 1L);

        verify(suppressionEntryRepository).delete(entry);
    }

    @Test
    void findSuppressionReasonShouldReturnOptionalReason() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SuppressionEntry entry = SuppressionEntry.builder()
                .workspace(workspace)
                .email("blocked@test.com")
                .reason(SuppressionReason.UNSUBSCRIBED)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(suppressionEntryRepository.findByWorkspaceIdAndEmailIgnoreCase(10L, "blocked@test.com"))
                .thenReturn(Optional.of(entry));

        Optional<SuppressionReason> result = suppressionService.findSuppressionReason("owner@test.com", "blocked@test.com");

        assertThat(result).contains(SuppressionReason.UNSUBSCRIBED);
    }
}
