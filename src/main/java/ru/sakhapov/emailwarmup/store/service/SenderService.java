package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.CreateSenderRequest;
import ru.sakhapov.emailwarmup.api.dto.SendTestEmailRequest;
import ru.sakhapov.emailwarmup.api.dto.SendTestEmailResponse;
import ru.sakhapov.emailwarmup.api.dto.SenderResponse;
import ru.sakhapov.emailwarmup.api.dto.SenderTestResponse;

import java.util.List;

public interface SenderService {

    SenderResponse createSender(String ownerEmail, CreateSenderRequest request);

    List<SenderResponse> listSenders(String ownerEmail);

    SenderTestResponse testSender(String ownerEmail, Long senderId);

    SenderTestResponse testImapSender(String ownerEmail, Long senderId);

    SendTestEmailResponse sendTestEmail(String ownerEmail, Long senderId, SendTestEmailRequest request);

    MailSendResult sendEmail(String ownerEmail, Long senderId, String to, String subject, String text);
}
