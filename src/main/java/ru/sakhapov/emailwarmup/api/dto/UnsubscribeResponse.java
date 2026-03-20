package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnsubscribeResponse {
    private boolean success;
    private String message;
    private String email;
}
