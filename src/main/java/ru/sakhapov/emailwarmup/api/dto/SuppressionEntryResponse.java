package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SuppressionEntryResponse {
    private Long id;
    private String email;
    private String reason;
    private Instant createdAt;
}
