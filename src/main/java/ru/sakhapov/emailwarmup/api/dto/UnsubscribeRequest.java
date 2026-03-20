package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnsubscribeRequest {

    @NotBlank
    private String token;
}
