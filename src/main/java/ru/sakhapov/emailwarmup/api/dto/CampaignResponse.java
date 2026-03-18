package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CampaignResponse {
    private Long id;
    private Long senderId;
    private String name;
    private String subject;
    private String text;
    private String status;
    private Instant createdAt;
}
