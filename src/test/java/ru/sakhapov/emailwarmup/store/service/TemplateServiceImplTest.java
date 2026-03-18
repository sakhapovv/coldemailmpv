package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import ru.sakhapov.emailwarmup.store.entity.Prospect;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateServiceImplTest {

    private final TemplateServiceImpl templateService = new TemplateServiceImpl();

    @Test
    void renderShouldReplaceKnownPlaceholders() {
        Prospect prospect = Prospect.builder()
                .email("john@acme.com")
                .firstName("John")
                .lastName("Doe")
                .company("Acme")
                .jobTitle("Founder")
                .website("https://acme.com")
                .build();

        String result = templateService.render(
                "Hi {{firstName}} {{lastName}} from {{company}} {{email}} {{jobTitle}} {{website}}",
                prospect
        );

        assertThat(result).isEqualTo("Hi John Doe from Acme john@acme.com Founder https://acme.com");
    }

    @Test
    void renderShouldReplaceNullValuesWithEmptyStrings() {
        Prospect prospect = Prospect.builder()
                .email("john@acme.com")
                .build();

        String result = templateService.render("Hi {{firstName}} from {{company}}", prospect);

        assertThat(result).isEqualTo("Hi  from ");
    }

    @Test
    void renderShouldReturnBlankInputAsIs() {
        Prospect prospect = Prospect.builder().build();

        assertThat(templateService.render(null, prospect)).isNull();
        assertThat(templateService.render("   ", prospect)).isEqualTo("   ");
    }
}
