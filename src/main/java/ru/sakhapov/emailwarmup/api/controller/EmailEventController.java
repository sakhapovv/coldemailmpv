package ru.sakhapov.emailwarmup.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.sakhapov.emailwarmup.api.dto.EmailEventResponse;
import ru.sakhapov.emailwarmup.store.service.EmailEventService;

import java.util.List;

@RestController
@RequestMapping("/api/email-events")
@RequiredArgsConstructor
public class EmailEventController {

    private final EmailEventService emailEventService;

    @GetMapping
    public List<EmailEventResponse> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return emailEventService.getWorkspaceEmailEvents(principal.getUsername());
    }
}
