package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendProspectEmailRequest {

    @NotNull
    private Long senderId;

    @NotBlank
    @Size(max = 255)
    private String subject;

    @NotBlank
    @Size(max = 10000)
    private String text;
}
