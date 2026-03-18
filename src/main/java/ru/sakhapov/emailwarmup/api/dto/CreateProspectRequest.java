package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProspectRequest {

    @Email
    @NotBlank
    private String email;

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String company;

    @Size(max = 255)
    private String jobTitle;

    @Size(max = 512)
    private String website;
}
