package ru.sakhapov.emailwarmup.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;

import java.util.List;
import java.util.Optional;

public interface SenderAccountRepository extends JpaRepository<SenderAccount, Long> {
    List<SenderAccount> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<SenderAccount> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByWorkspaceIdAndEmailIgnoreCase(Long workspaceId, String email);
}
