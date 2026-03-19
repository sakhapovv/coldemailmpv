package ru.sakhapov.emailwarmup.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sakhapov.emailwarmup.store.entity.SuppressionEntry;

import java.util.List;
import java.util.Optional;

public interface SuppressionEntryRepository extends JpaRepository<SuppressionEntry, Long> {
    List<SuppressionEntry> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<SuppressionEntry> findByWorkspaceIdAndEmailIgnoreCase(Long workspaceId, String email);
    Optional<SuppressionEntry> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByWorkspaceIdAndEmailIgnoreCase(Long workspaceId, String email);
}
