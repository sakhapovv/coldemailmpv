package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class UserMeResponse {
    private Long id;
    private String email;
    private List<String> roles;
    private Instant createdAt;
}
