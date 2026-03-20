package ru.sakhapov.emailwarmup.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.sakhapov.emailwarmup.api.dto.UnsubscribeRequest;
import ru.sakhapov.emailwarmup.api.dto.UnsubscribeResponse;
import ru.sakhapov.emailwarmup.store.service.UnsubscribeService;

@RestController
@RequestMapping("/api/public/unsubscribe")
@RequiredArgsConstructor
public class PublicUnsubscribeController {

    private final UnsubscribeService unsubscribeService;

    @GetMapping
    public UnsubscribeResponse unsubscribeGet(@RequestParam String token) {
        return unsubscribeService.unsubscribe(token);
    }

    @PostMapping
    public UnsubscribeResponse unsubscribePost(@Valid @RequestBody UnsubscribeRequest request) {
        return unsubscribeService.unsubscribe(request.getToken());
    }
}
