package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSenderRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String smtpHost;

    @Min(1)
    @Max(65535)
    private Integer smtpPort;

    @NotBlank
    private String smtpUsername;

    @NotBlank
    private String smtpPassword;

    private String fromName;

    private boolean startTls = true;

    private boolean ssl = false;
}
