package ru.sakhapov.emailwarmup.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sakhapov.emailwarmup.store.entity.Workspace;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findFirstByOwnerEmail(String ownerEmail);
}
