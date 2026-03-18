package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ProspectResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String company;
    private String jobTitle;
    private String website;
    private String status;
    private Instant createdAt;
}
