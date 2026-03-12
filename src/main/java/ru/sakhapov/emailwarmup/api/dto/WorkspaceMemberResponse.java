package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class WorkspaceMemberResponse {
    private Long userId;
    private String email;
    private String role;
    private Instant joinedAt;
}
