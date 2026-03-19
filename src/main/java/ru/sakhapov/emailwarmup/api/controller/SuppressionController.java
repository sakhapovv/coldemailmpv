package ru.sakhapov.emailwarmup.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.sakhapov.emailwarmup.api.dto.CreateSuppressionRequest;
import ru.sakhapov.emailwarmup.api.dto.SuppressionEntryResponse;
import ru.sakhapov.emailwarmup.store.service.SuppressionService;

import java.util.List;

@RestController
@RequestMapping("/api/suppressions")
@RequiredArgsConstructor
public class SuppressionController {

    private final SuppressionService suppressionService;

    @PostMapping
    public SuppressionEntryResponse create(@AuthenticationPrincipal UserDetails principal,
                                           @Valid @RequestBody CreateSuppressionRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return suppressionService.createSuppression(principal.getUsername(), request);
    }

    @GetMapping
    public List<SuppressionEntryResponse> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return suppressionService.listSuppressions(principal.getUsername());
    }

    @DeleteMapping("/{suppressionId}")
    public void delete(@AuthenticationPrincipal UserDetails principal,
                       @PathVariable Long suppressionId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        suppressionService.deleteSuppression(principal.getUsername(), suppressionId);
    }
}
