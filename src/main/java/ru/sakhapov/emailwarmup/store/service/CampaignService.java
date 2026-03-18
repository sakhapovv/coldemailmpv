package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.CampaignResponse;
import ru.sakhapov.emailwarmup.api.dto.CampaignSendResponse;
import ru.sakhapov.emailwarmup.api.dto.CreateCampaignRequest;

import java.util.List;

public interface CampaignService {

    CampaignResponse createCampaign(String ownerEmail, CreateCampaignRequest request);

    List<CampaignResponse> listCampaigns(String ownerEmail);

    CampaignResponse getCampaign(String ownerEmail, Long campaignId);

    CampaignSendResponse runCampaign(String ownerEmail, Long campaignId);
}
