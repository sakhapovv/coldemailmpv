package ru.sakhapov.emailwarmup.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.ProspectStatus;

import java.util.List;
import java.util.Optional;

public interface ProspectRepository extends JpaRepository<Prospect, Long> {
    List<Prospect> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    List<Prospect> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, ProspectStatus status);
    Optional<Prospect> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByWorkspaceIdAndEmailIgnoreCase(Long workspaceId, String email);
}
