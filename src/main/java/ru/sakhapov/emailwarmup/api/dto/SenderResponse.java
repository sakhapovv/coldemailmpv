package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SenderResponse {
    private Long id;
    private String email;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String fromName;
    private boolean startTls;
    private boolean ssl;
    private String status;
    private Instant createdAt;
}
