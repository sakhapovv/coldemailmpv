package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EmailEventResponse {
    private Long id;
    private Long senderId;
    private Long prospectId;
    private String toEmail;
    private String subject;
    private String status;
    private String providerMessageId;
    private String errorMessage;
    private Instant createdAt;
}
