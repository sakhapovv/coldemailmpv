package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.store.entity.Prospect;

public interface TemplateService {

    String render(String template, Prospect prospect);
}
