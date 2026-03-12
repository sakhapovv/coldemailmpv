package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SenderTestResponse {
    private boolean success;
    private String message;
}
