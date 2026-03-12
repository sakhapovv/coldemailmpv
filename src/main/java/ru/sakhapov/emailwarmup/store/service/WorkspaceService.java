package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceMemberResponse;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceResponse;
import ru.sakhapov.emailwarmup.store.entity.User;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceMember;
import ru.sakhapov.emailwarmup.store.entity.WorkspaceRole;
import ru.sakhapov.emailwarmup.store.repository.UserRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceMemberRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceResponse getCurrentWorkspace(String userEmail) {
        Workspace workspace = getOwnedWorkspace(userEmail);

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .ownerUserId(workspace.getOwner().getId())
                .createdAt(workspace.getCreatedAt())
                .build();
    }

    @Transactional
    public WorkspaceMemberResponse addMemberToCurrentWorkspace(String ownerEmail, String inviteeEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        User invitee = userRepository.findByEmail(inviteeEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + inviteeEmail));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), invitee.getId())
                .orElseGet(() -> workspaceMemberRepository.save(
                        WorkspaceMember.builder()
                                .workspace(workspace)
                                .user(invitee)
                                .role(WorkspaceRole.MEMBER)
                                .build()
                ));

        return mapMember(member);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getCurrentWorkspaceMembers(String ownerEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return workspaceMemberRepository.findAllByWorkspaceIdOrderByCreatedAtAsc(workspace.getId()).stream()
                .map(this::mapMember)
                .toList();
    }

    @Transactional
    public void removeMemberFromCurrentWorkspace(String ownerEmail, Long userId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Owner cannot be removed");
        }

        workspaceMemberRepository.delete(member);
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private WorkspaceMemberResponse mapMember(WorkspaceMember member) {
        return WorkspaceMemberResponse.builder()
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .role(member.getRole().name())
                .joinedAt(member.getCreatedAt())
                .build();
    }
}
