package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteWorkspaceMemberRequest {

    @Email
    @NotBlank
    private String email;
}
