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
import ru.sakhapov.emailwarmup.api.dto.CampaignResponse;
import ru.sakhapov.emailwarmup.api.dto.CampaignSendResponse;
import ru.sakhapov.emailwarmup.api.dto.CreateCampaignRequest;
import ru.sakhapov.emailwarmup.store.service.CampaignService;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public CampaignResponse create(@AuthenticationPrincipal UserDetails principal,
                                   @Valid @RequestBody CreateCampaignRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return campaignService.createCampaign(principal.getUsername(), request);
    }

    @GetMapping
    public List<CampaignResponse> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return campaignService.listCampaigns(principal.getUsername());
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse get(@AuthenticationPrincipal UserDetails principal,
                                @PathVariable Long campaignId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return campaignService.getCampaign(principal.getUsername(), campaignId);
    }

    @PostMapping("/{campaignId}/send")
    public CampaignSendResponse send(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long campaignId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return campaignService.runCampaign(principal.getUsername(), campaignId);
    }
}
