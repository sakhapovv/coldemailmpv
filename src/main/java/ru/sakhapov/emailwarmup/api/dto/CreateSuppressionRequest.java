package ru.sakhapov.emailwarmup.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;

@Getter
@Setter
public class CreateSuppressionRequest {

    @Email
    @NotBlank
    private String email;

    @NotNull
    private SuppressionReason reason;
}
