package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.EmailEventResponse;
import ru.sakhapov.emailwarmup.api.dto.SendProspectEmailRequest;

import java.util.List;

public interface EmailEventService {

    EmailEventResponse sendToProspect(String ownerEmail, Long prospectId, SendProspectEmailRequest request);

    List<EmailEventResponse> getProspectEmailEvents(String ownerEmail, Long prospectId);

    List<EmailEventResponse> getWorkspaceEmailEvents(String ownerEmail);
}
