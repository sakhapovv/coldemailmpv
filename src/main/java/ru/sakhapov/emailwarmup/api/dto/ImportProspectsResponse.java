package ru.sakhapov.emailwarmup.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ImportProspectsResponse(
        int importedCount,
        int skippedCount,
        List<String> errors
) {
}
