package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sakhapov.emailwarmup.api.dto.CreateProspectRequest;
import ru.sakhapov.emailwarmup.api.dto.ImportProspectsResponse;
import ru.sakhapov.emailwarmup.api.dto.ProspectResponse;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.ProspectStatus;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProspectServiceImpl implements ProspectService {

    private final ProspectRepository prospectRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public ProspectResponse createProspect(String ownerEmail, CreateProspectRequest request) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        if (prospectRepository.existsByWorkspaceIdAndEmailIgnoreCase(workspace.getId(), request.getEmail())) {
            throw new IllegalArgumentException("Prospect with this email already exists in workspace");
        }

        Prospect prospect = prospectRepository.save(
                Prospect.builder()
                        .workspace(workspace)
                        .email(request.getEmail())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .company(request.getCompany())
                        .jobTitle(request.getJobTitle())
                        .website(request.getWebsite())
                        .status(ProspectStatus.ACTIVE)
                        .build()
        );

        return map(prospect);
    }

    @Transactional
    public ImportProspectsResponse importProspects(String ownerEmail, MultipartFile file) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        List<String> errors = new ArrayList<>();
        int importedCount = 0;
        int skippedCount = 0;
        Set<String> seenEmails = prospectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .map(Prospect::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase())
                .collect(Collectors.toSet());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file must contain a header row");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(headerLine));
            validateRequiredHeaders(headerIndex);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    skippedCount++;
                    continue;
                }

                try {
                    List<String> values = parseCsvLine(line);
                    String email = getCsvValue(values, headerIndex, "email");
                    if (email == null || email.isBlank()) {
                        skippedCount++;
                        errors.add("Row " + rowNumber + ": email is required");
                        continue;
                    }

                    String normalizedEmail = email.trim().toLowerCase();
                    if (seenEmails.contains(normalizedEmail)) {
                        skippedCount++;
                        continue;
                    }

                    Prospect prospect = Prospect.builder()
                            .workspace(workspace)
                            .email(email.trim())
                            .firstName(getCsvValue(values, headerIndex, "firstName"))
                            .lastName(getCsvValue(values, headerIndex, "lastName"))
                            .company(getCsvValue(values, headerIndex, "company"))
                            .jobTitle(getCsvValue(values, headerIndex, "jobTitle"))
                            .website(getCsvValue(values, headerIndex, "website"))
                            .status(ProspectStatus.ACTIVE)
                            .build();
                    prospectRepository.save(prospect);
                    seenEmails.add(normalizedEmail);
                    importedCount++;
                } catch (IllegalArgumentException exception) {
                    skippedCount++;
                    errors.add("Row " + rowNumber + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read CSV file");
        }

        return ImportProspectsResponse.builder()
                .importedCount(importedCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProspectResponse> listProspects(String ownerEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return prospectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProspectResponse getProspect(String ownerEmail, Long prospectId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        Prospect prospect = prospectRepository.findByIdAndWorkspaceId(prospectId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect not found"));
        return map(prospect);
    }

    @Transactional
    public void deleteProspect(String ownerEmail, Long prospectId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        Prospect prospect = prospectRepository.findByIdAndWorkspaceId(prospectId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect not found"));
        prospectRepository.delete(prospect);
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private ProspectResponse map(Prospect prospect) {
        return ProspectResponse.builder()
                .id(prospect.getId())
                .email(prospect.getEmail())
                .firstName(prospect.getFirstName())
                .lastName(prospect.getLastName())
                .company(prospect.getCompany())
                .jobTitle(prospect.getJobTitle())
                .website(prospect.getWebsite())
                .status(prospect.getStatus().name())
                .createdAt(prospect.getCreatedAt())
                .build();
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (header != null && !header.isBlank()) {
                headerIndex.put(header.trim(), index);
            }
        }
        return headerIndex;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("email")) {
            throw new IllegalArgumentException("CSV header must include email");
        }
    }

    private String getCsvValue(List<String> values, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key);
        if (index == null || index >= values.size()) {
            return null;
        }
        String value = values.get(index);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char symbol = line.charAt(index);
            if (symbol == '"') {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
                continue;
            }

            if (symbol == ',' && !insideQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(symbol);
        }

        if (insideQuotes) {
            throw new IllegalArgumentException("unclosed quoted value");
        }

        values.add(current.toString());
        return values;
    }
}
