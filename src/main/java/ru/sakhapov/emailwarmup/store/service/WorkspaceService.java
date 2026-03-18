package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.WorkspaceMemberResponse;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceResponse;

import java.util.List;

public interface WorkspaceService {

    WorkspaceResponse getCurrentWorkspace(String userEmail);

    WorkspaceMemberResponse addMemberToCurrentWorkspace(String ownerEmail, String inviteeEmail);

    List<WorkspaceMemberResponse> getCurrentWorkspaceMembers(String ownerEmail);

    void removeMemberFromCurrentWorkspace(String ownerEmail, Long userId);
}
