package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.UnsubscribeResponse;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.ProspectStatus;
import ru.sakhapov.emailwarmup.store.entity.SuppressionEntry;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.SuppressionEntryRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsubscribeServiceImplTest {

    @Mock
    private ProspectRepository prospectRepository;
    @Mock
    private SuppressionEntryRepository suppressionEntryRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;

    private UnsubscribeServiceImpl unsubscribeService;

    @BeforeEach
    void setUp() {
        unsubscribeService = new UnsubscribeServiceImpl(
                prospectRepository,
                suppressionEntryRepository,
                workspaceRepository,
                "http://localhost:8080",
                "Q29kZXhFbWFpbFdhcm11cFRlc3RTZWNyZXRLZXlGb3JKV1QxMjM0NTY3ODkw",
                2592000000L
        );
    }

    @Test
    void buildUnsubscribeUrlShouldReturnPublicLinkWithToken() {
        Workspace workspace = Workspace.builder().id(10L).build();
        Prospect prospect = Prospect.builder()
                .id(1L)
                .workspace(workspace)
                .email("user@test.com")
                .build();

        String url = unsubscribeService.buildUnsubscribeUrl(prospect);

        assertThat(url).startsWith("http://localhost:8080/api/public/unsubscribe?token=");
    }

    @Test
    void unsubscribeShouldCreateSuppressionAndMarkProspectUnsubscribed() {
        Workspace workspace = Workspace.builder().id(10L).build();
        Prospect prospect = Prospect.builder()
                .id(1L)
                .workspace(workspace)
                .email("user@test.com")
                .status(ProspectStatus.ACTIVE)
                .build();
        String token = unsubscribeService.buildUnsubscribeUrl(prospect).replace(
                "http://localhost:8080/api/public/unsubscribe?token=",
                ""
        );

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(suppressionEntryRepository.findByWorkspaceIdAndEmailIgnoreCase(10L, "user@test.com"))
                .thenReturn(Optional.empty());
        when(suppressionEntryRepository.save(any(SuppressionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(prospectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(prospect));

        UnsubscribeResponse response = unsubscribeService.unsubscribe(token);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getMessage()).isEqualTo("Email unsubscribed successfully");
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.UNSUBSCRIBED);
        verify(suppressionEntryRepository).save(any(SuppressionEntry.class));
    }
}
