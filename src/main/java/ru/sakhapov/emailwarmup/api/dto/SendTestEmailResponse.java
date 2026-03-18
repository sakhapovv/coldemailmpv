package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SendTestEmailResponse {
    private boolean success;
    private String message;
    private String messageId;
    private Instant sentAt;
}
