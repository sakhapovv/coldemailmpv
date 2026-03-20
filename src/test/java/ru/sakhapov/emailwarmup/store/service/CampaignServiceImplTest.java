package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.CampaignResponse;
import ru.sakhapov.emailwarmup.api.dto.CampaignSendResponse;
import ru.sakhapov.emailwarmup.api.dto.CreateCampaignRequest;
import ru.sakhapov.emailwarmup.api.dto.SendProspectEmailRequest;
import ru.sakhapov.emailwarmup.store.entity.Campaign;
import ru.sakhapov.emailwarmup.store.entity.CampaignStatus;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.ProspectStatus;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.CampaignRepository;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.SenderAccountRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private SenderAccountRepository senderAccountRepository;
    @Mock
    private ProspectRepository prospectRepository;
    @Mock
    private EmailEventService emailEventService;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    @Test
    void createCampaignShouldPersistDraftCampaign() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SenderAccount sender = SenderAccount.builder().id(5L).workspace(workspace).build();
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setSenderId(5L);
        request.setName("First campaign");
        request.setSubject("Hello {{firstName}}");
        request.setText("Hi {{firstName}}");

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(senderAccountRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(sender));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign campaign = invocation.getArgument(0);
            campaign.setId(100L);
            return campaign;
        });

        CampaignResponse response = campaignService.createCampaign("owner@test.com", request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getSenderId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("First campaign");
        assertThat(response.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void getCampaignShouldFailWhenMissing() {
        Workspace workspace = Workspace.builder().id(10L).build();
        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(campaignRepository.findByIdAndWorkspaceId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getCampaign("owner@test.com", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Campaign not found");
    }

    @Test
    void listCampaignsShouldReturnMappedCampaigns() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SenderAccount sender = SenderAccount.builder().id(5L).workspace(workspace).build();
        Campaign campaign = Campaign.builder()
                .id(100L)
                .workspace(workspace)
                .senderAccount(sender)
                .name("First campaign")
                .subject("Hello")
                .text("Hi")
                .status(CampaignStatus.DRAFT)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(campaignRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(campaign));

        List<CampaignResponse> responses = campaignService.listCampaigns("owner@test.com");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getName()).isEqualTo("First campaign");
    }

    @Test
    void runCampaignShouldCountSuccessesAndFailures() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SenderAccount sender = SenderAccount.builder().id(5L).workspace(workspace).build();
        Campaign campaign = Campaign.builder()
                .id(100L)
                .workspace(workspace)
                .senderAccount(sender)
                .name("First campaign")
                .subject("Hello {{firstName}}")
                .text("Hi {{firstName}}")
                .status(CampaignStatus.DRAFT)
                .build();
        Prospect first = Prospect.builder().id(1L).status(ProspectStatus.ACTIVE).build();
        Prospect second = Prospect.builder().id(2L).status(ProspectStatus.ACTIVE).build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(campaignRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(campaign));
        when(prospectRepository.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(10L, ProspectStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(emailEventService.sendToProspect(anyString(), anyLong(), any(SendProspectEmailRequest.class)))
                .thenAnswer(invocation -> {
                    Long prospectId = invocation.getArgument(1);
                    if (prospectId.equals(2L)) {
                        throw new IllegalArgumentException("Send failed");
                    }
                    return null;
                });

        CampaignSendResponse response = campaignService.runCampaign("owner@test.com", 100L);

        assertThat(response.getCampaignId()).isEqualTo(100L);
        assertThat(response.getTotalProspects()).isEqualTo(2);
        assertThat(response.getSentCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isEqualTo(0);
        assertThat(response.getFailedCount()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        ArgumentCaptor<SendProspectEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendProspectEmailRequest.class);
        verify(emailEventService, times(2))
                .sendToProspect(any(String.class), any(Long.class), requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SendProspectEmailRequest::getSenderId)
                .containsOnly(5L);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
    }

    @Test
    void runCampaignShouldCountSkippedSeparately() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SenderAccount sender = SenderAccount.builder().id(5L).workspace(workspace).build();
        Campaign campaign = Campaign.builder()
                .id(100L)
                .workspace(workspace)
                .senderAccount(sender)
                .name("First campaign")
                .subject("Hello")
                .text("Hi")
                .status(CampaignStatus.DRAFT)
                .build();
        Prospect prospect = Prospect.builder().id(1L).status(ProspectStatus.ACTIVE).build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(campaignRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(campaign));
        when(prospectRepository.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(10L, ProspectStatus.ACTIVE))
                .thenReturn(List.of(prospect));
        when(emailEventService.sendToProspect(anyString(), anyLong(), any(SendProspectEmailRequest.class)))
                .thenThrow(new IllegalArgumentException("Recipient is suppressed: MANUAL (eventId=1)"));

        CampaignSendResponse response = campaignService.runCampaign("owner@test.com", 100L);

        assertThat(response.getSentCount()).isEqualTo(0);
        assertThat(response.getSkippedCount()).isEqualTo(1);
        assertThat(response.getFailedCount()).isEqualTo(0);
    }
}
