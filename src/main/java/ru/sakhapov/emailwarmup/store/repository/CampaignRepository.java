package ru.sakhapov.emailwarmup.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sakhapov.emailwarmup.store.entity.Campaign;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<Campaign> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
