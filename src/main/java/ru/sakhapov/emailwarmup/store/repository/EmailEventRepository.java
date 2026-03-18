package ru.sakhapov.emailwarmup.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sakhapov.emailwarmup.store.entity.EmailEvent;

import java.util.List;

public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {
    List<EmailEvent> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    List<EmailEvent> findAllByProspectIdOrderByCreatedAtDesc(Long prospectId);
}
