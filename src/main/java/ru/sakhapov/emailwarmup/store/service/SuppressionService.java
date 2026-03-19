package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.CreateSuppressionRequest;
import ru.sakhapov.emailwarmup.api.dto.SuppressionEntryResponse;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;

import java.util.List;
import java.util.Optional;

public interface SuppressionService {

    SuppressionEntryResponse createSuppression(String ownerEmail, CreateSuppressionRequest request);

    List<SuppressionEntryResponse> listSuppressions(String ownerEmail);

    void deleteSuppression(String ownerEmail, Long suppressionId);

    Optional<SuppressionReason> findSuppressionReason(String ownerEmail, String email);
}
