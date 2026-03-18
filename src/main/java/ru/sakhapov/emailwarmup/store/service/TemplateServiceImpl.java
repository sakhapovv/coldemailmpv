package ru.sakhapov.emailwarmup.store.service;

import org.springframework.stereotype.Service;
import ru.sakhapov.emailwarmup.store.entity.Prospect;

import java.util.Map;

@Service
public class TemplateServiceImpl implements TemplateService {

    public String render(String template, Prospect prospect) {
        if (template == null || template.isBlank()) {
            return template;
        }

        String rendered = template;
        for (Map.Entry<String, String> entry : values(prospect).entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private Map<String, String> values(Prospect prospect) {
        return Map.of(
                "email", safe(prospect.getEmail()),
                "firstName", safe(prospect.getFirstName()),
                "lastName", safe(prospect.getLastName()),
                "company", safe(prospect.getCompany()),
                "jobTitle", safe(prospect.getJobTitle()),
                "website", safe(prospect.getWebsite())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
