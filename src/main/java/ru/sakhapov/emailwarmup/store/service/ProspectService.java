package ru.sakhapov.emailwarmup.store.service;

import org.springframework.web.multipart.MultipartFile;
import ru.sakhapov.emailwarmup.api.dto.CreateProspectRequest;
import ru.sakhapov.emailwarmup.api.dto.ImportProspectsResponse;
import ru.sakhapov.emailwarmup.api.dto.ProspectResponse;

import java.util.List;

public interface ProspectService {

    ProspectResponse createProspect(String ownerEmail, CreateProspectRequest request);

    ImportProspectsResponse importProspects(String ownerEmail, MultipartFile file);

    List<ProspectResponse> listProspects(String ownerEmail);

    ProspectResponse getProspect(String ownerEmail, Long prospectId);

    void deleteProspect(String ownerEmail, Long prospectId);
}
