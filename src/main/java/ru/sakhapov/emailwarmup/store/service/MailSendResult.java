package ru.sakhapov.emailwarmup.store.service;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MailSendResult {
    private String messageId;
    private Instant sentAt;
}
