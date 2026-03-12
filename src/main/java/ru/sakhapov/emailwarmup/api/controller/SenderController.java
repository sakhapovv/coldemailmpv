package ru.sakhapov.emailwarmup.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.sakhapov.emailwarmup.api.dto.CreateSenderRequest;
import ru.sakhapov.emailwarmup.api.dto.SenderResponse;
import ru.sakhapov.emailwarmup.api.dto.SenderTestResponse;
import ru.sakhapov.emailwarmup.store.service.SenderService;

import java.util.List;

@RestController
@RequestMapping("/api/senders")
@RequiredArgsConstructor
public class SenderController {

    private final SenderService senderService;

    @PostMapping
    public SenderResponse create(@AuthenticationPrincipal UserDetails principal,
                                 @Valid @RequestBody CreateSenderRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return senderService.createSender(principal.getUsername(), request);
    }

    @GetMapping
    public List<SenderResponse> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return senderService.listSenders(principal.getUsername());
    }

    @PostMapping("/{senderId}/test")
    public SenderTestResponse test(@AuthenticationPrincipal UserDetails principal,
                                   @PathVariable Long senderId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return senderService.testSender(principal.getUsername(), senderId);
    }
}
