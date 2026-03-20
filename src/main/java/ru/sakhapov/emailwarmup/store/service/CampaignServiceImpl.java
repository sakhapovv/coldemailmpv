package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SenderAccountRepository senderAccountRepository;
    private final ProspectRepository prospectRepository;
    private final EmailEventService emailEventService;

    @Transactional
    public CampaignResponse createCampaign(String ownerEmail, CreateCampaignRequest request) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        SenderAccount sender = senderAccountRepository.findByIdAndWorkspaceId(request.getSenderId(), workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        Campaign campaign = campaignRepository.save(
                Campaign.builder()
                        .workspace(workspace)
                        .senderAccount(sender)
                        .name(request.getName())
                        .subject(request.getSubject())
                        .text(request.getText())
                        .status(CampaignStatus.DRAFT)
                        .build()
        );

        return map(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaigns(String ownerEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return campaignRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(String ownerEmail, Long campaignId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        Campaign campaign = campaignRepository.findByIdAndWorkspaceId(campaignId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        return map(campaign);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public CampaignSendResponse runCampaign(String ownerEmail, Long campaignId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        Campaign campaign = campaignRepository.findByIdAndWorkspaceId(campaignId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        campaign.setStatus(CampaignStatus.RUNNING);

        List<Prospect> prospects = prospectRepository.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(
                workspace.getId(),
                ProspectStatus.ACTIVE
        );

        int sentCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Prospect prospect : prospects) {
            try {
                emailEventService.sendToProspect(
                        ownerEmail,
                        prospect.getId(),
                        buildSendRequest(campaign)
                );
                sentCount++;
            } catch (IllegalArgumentException ex) {
                if (isSkipped(ex.getMessage())) {
                    skippedCount++;
                } else {
                    failedCount++;
                }
            }
        }

        campaign.setStatus(CampaignStatus.COMPLETED);

        return CampaignSendResponse.builder()
                .campaignId(campaign.getId())
                .totalProspects(prospects.size())
                .sentCount(sentCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .status(campaign.getStatus().name())
                .build();
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private SendProspectEmailRequest buildSendRequest(Campaign campaign) {
        SendProspectEmailRequest request = new SendProspectEmailRequest();
        request.setSenderId(campaign.getSenderAccount().getId());
        request.setSubject(campaign.getSubject());
        request.setText(campaign.getText());
        return request;
    }

    private CampaignResponse map(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .senderId(campaign.getSenderAccount().getId())
                .name(campaign.getName())
                .subject(campaign.getSubject())
                .text(campaign.getText())
                .status(campaign.getStatus().name())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    private boolean isSkipped(String message) {
        return message != null && message.startsWith("Recipient is suppressed:");
    }
}
