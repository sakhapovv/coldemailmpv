package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendTestEmailRequest {

    @Email
    @NotBlank
    private String to;

    @NotBlank
    @Size(max = 255)
    private String subject;

    @NotBlank
    @Size(max = 10000)
    private String text;
}
