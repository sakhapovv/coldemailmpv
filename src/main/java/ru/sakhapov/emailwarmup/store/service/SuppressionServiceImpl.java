package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.CreateSuppressionRequest;
import ru.sakhapov.emailwarmup.api.dto.SuppressionEntryResponse;
import ru.sakhapov.emailwarmup.store.entity.SuppressionEntry;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.SuppressionEntryRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuppressionServiceImpl implements SuppressionService {

    private final SuppressionEntryRepository suppressionEntryRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public SuppressionEntryResponse createSuppression(String ownerEmail, CreateSuppressionRequest request) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        if (suppressionEntryRepository.existsByWorkspaceIdAndEmailIgnoreCase(workspace.getId(), request.getEmail())) {
            throw new IllegalArgumentException("Email is already suppressed in workspace");
        }

        SuppressionEntry entry = suppressionEntryRepository.save(
                SuppressionEntry.builder()
                        .workspace(workspace)
                        .email(request.getEmail().trim())
                        .reason(request.getReason())
                        .build()
        );

        return map(entry);
    }

    @Transactional(readOnly = true)
    public List<SuppressionEntryResponse> listSuppressions(String ownerEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return suppressionEntryRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public void deleteSuppression(String ownerEmail, Long suppressionId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        SuppressionEntry entry = suppressionEntryRepository.findByIdAndWorkspaceId(suppressionId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Suppression not found"));
        suppressionEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public Optional<SuppressionReason> findSuppressionReason(String ownerEmail, String email) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return suppressionEntryRepository.findByWorkspaceIdAndEmailIgnoreCase(workspace.getId(), email)
                .map(SuppressionEntry::getReason);
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private SuppressionEntryResponse map(SuppressionEntry entry) {
        return SuppressionEntryResponse.builder()
                .id(entry.getId())
                .email(entry.getEmail())
                .reason(entry.getReason().name())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
