package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.UnsubscribeResponse;
import ru.sakhapov.emailwarmup.store.entity.Prospect;

public interface UnsubscribeService {

    String buildUnsubscribeUrl(Prospect prospect);

    UnsubscribeResponse unsubscribe(String token);
}
