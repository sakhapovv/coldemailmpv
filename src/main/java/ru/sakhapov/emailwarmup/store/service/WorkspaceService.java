package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sakhapov.emailwarmup.api.dto.WorkspaceResponse;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceResponse getCurrentWorkspace(String userEmail) {
        Workspace workspace = workspaceRepository.findFirstByOwnerEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .ownerUserId(workspace.getOwner().getId())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}
