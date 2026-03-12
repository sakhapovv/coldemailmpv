package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class WorkspaceResponse {
    private Long id;
    private String name;
    private Long ownerUserId;
    private Instant createdAt;
}
