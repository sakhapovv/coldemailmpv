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
import ru.sakhapov.emailwarmup.api.dto.InviteWorkspaceMemberRequest;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceMemberResponse;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceResponse;
import ru.sakhapov.emailwarmup.store.service.WorkspaceService;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/current")
    public WorkspaceResponse current(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return workspaceService.getCurrentWorkspace(principal.getUsername());
    }

    @PostMapping("/current/members")
    public WorkspaceMemberResponse addMember(@AuthenticationPrincipal UserDetails principal,
                                             @Valid @RequestBody InviteWorkspaceMemberRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return workspaceService.addMemberToCurrentWorkspace(principal.getUsername(), request.getEmail());
    }

    @GetMapping("/current/members")
    public List<WorkspaceMemberResponse> currentMembers(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return workspaceService.getCurrentWorkspaceMembers(principal.getUsername());
    }

    @DeleteMapping("/current/members/{userId}")
    public void removeMember(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long userId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        workspaceService.removeMemberFromCurrentWorkspace(principal.getUsername(), userId);
    }
}
