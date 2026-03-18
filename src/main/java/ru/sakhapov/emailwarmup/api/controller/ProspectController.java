package ru.sakhapov.emailwarmup.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.sakhapov.emailwarmup.api.dto.CreateProspectRequest;
import ru.sakhapov.emailwarmup.api.dto.EmailEventResponse;
import ru.sakhapov.emailwarmup.api.dto.ImportProspectsResponse;
import ru.sakhapov.emailwarmup.api.dto.ProspectResponse;
import ru.sakhapov.emailwarmup.api.dto.SendProspectEmailRequest;
import ru.sakhapov.emailwarmup.store.service.EmailEventService;
import ru.sakhapov.emailwarmup.store.service.ProspectService;

import java.util.List;

@RestController
@RequestMapping("/api/prospects")
@RequiredArgsConstructor
public class ProspectController {

    private final ProspectService prospectService;
    private final EmailEventService emailEventService;

    @PostMapping
    public ProspectResponse create(@AuthenticationPrincipal UserDetails principal,
                                   @Valid @RequestBody CreateProspectRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return prospectService.createProspect(principal.getUsername(), request);
    }

    @PostMapping("/import")
    public ImportProspectsResponse importCsv(@AuthenticationPrincipal UserDetails principal,
                                             @RequestPart("file") MultipartFile file) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return prospectService.importProspects(principal.getUsername(), file);
    }

    @GetMapping
    public List<ProspectResponse> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return prospectService.listProspects(principal.getUsername());
    }

    @GetMapping("/{prospectId}")
    public ProspectResponse get(@AuthenticationPrincipal UserDetails principal,
                                @PathVariable Long prospectId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return prospectService.getProspect(principal.getUsername(), prospectId);
    }

    @DeleteMapping("/{prospectId}")
    public void delete(@AuthenticationPrincipal UserDetails principal,
                       @PathVariable Long prospectId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        prospectService.deleteProspect(principal.getUsername(), prospectId);
    }

    @PostMapping("/{prospectId}/send")
    public EmailEventResponse send(@AuthenticationPrincipal UserDetails principal,
                                   @PathVariable Long prospectId,
                                   @Valid @RequestBody SendProspectEmailRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return emailEventService.sendToProspect(principal.getUsername(), prospectId, request);
    }

    @GetMapping("/{prospectId}/events")
    public List<EmailEventResponse> events(@AuthenticationPrincipal UserDetails principal,
                                           @PathVariable Long prospectId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return emailEventService.getProspectEmailEvents(principal.getUsername(), prospectId);
    }
}
