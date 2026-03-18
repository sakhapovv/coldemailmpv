package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignSendResponse {
    private Long campaignId;
    private int totalProspects;
    private int sentCount;
    private int failedCount;
    private String status;
}
