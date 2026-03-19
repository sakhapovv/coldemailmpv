package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.EmailEventResponse;
import ru.sakhapov.emailwarmup.api.dto.SendProspectEmailRequest;
import ru.sakhapov.emailwarmup.store.entity.EmailEvent;
import ru.sakhapov.emailwarmup.store.entity.EmailEventStatus;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.EmailEventRepository;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.SenderAccountRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailEventServiceImpl implements EmailEventService {

    private final EmailEventRepository emailEventRepository;
    private final ProspectRepository prospectRepository;
    private final SenderAccountRepository senderAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SenderService senderService;
    private final TemplateService templateService;
    private final SuppressionService suppressionService;

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public EmailEventResponse sendToProspect(String ownerEmail, Long prospectId, SendProspectEmailRequest request) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        Prospect prospect = prospectRepository.findByIdAndWorkspaceId(prospectId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect not found"));
        SenderAccount sender = senderAccountRepository.findByIdAndWorkspaceId(request.getSenderId(), workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        String renderedSubject = templateService.render(request.getSubject(), prospect);
        String renderedText = templateService.render(request.getText(), prospect);
        var suppressionReason = suppressionService.findSuppressionReason(ownerEmail, prospect.getEmail());
        if (suppressionReason.isPresent()) {
            EmailEvent event = emailEventRepository.save(
                    EmailEvent.builder()
                            .workspace(workspace)
                            .senderAccount(sender)
                            .prospect(prospect)
                            .toEmail(prospect.getEmail())
                            .subject(renderedSubject)
                            .status(EmailEventStatus.SKIPPED)
                            .errorMessage("Suppressed: " + suppressionReason.get().name())
                            .build()
            );

            throw new IllegalArgumentException(
                    buildSuppressedMessage(suppressionReason.get()) + " (eventId=" + event.getId() + ")"
            );
        }

        try {
            MailSendResult result = senderService.sendEmail(
                    ownerEmail,
                    sender.getId(),
                    prospect.getEmail(),
                    renderedSubject,
                    renderedText
            );

            EmailEvent event = emailEventRepository.save(
                    EmailEvent.builder()
                            .workspace(workspace)
                            .senderAccount(sender)
                            .prospect(prospect)
                            .toEmail(prospect.getEmail())
                            .subject(renderedSubject)
                            .status(EmailEventStatus.SENT)
                            .providerMessageId(result.getMessageId())
                            .build()
            );

            return map(event);
        } catch (IllegalArgumentException ex) {
            EmailEvent event = emailEventRepository.save(
                    EmailEvent.builder()
                            .workspace(workspace)
                            .senderAccount(sender)
                            .prospect(prospect)
                            .toEmail(prospect.getEmail())
                            .subject(renderedSubject)
                            .status(EmailEventStatus.FAILED)
                            .errorMessage(truncate(ex.getMessage(), 1000))
                            .build()
            );

            throw new IllegalArgumentException(
                    ex.getMessage() + " (eventId=" + event.getId() + ")"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<EmailEventResponse> getProspectEmailEvents(String ownerEmail, Long prospectId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        Prospect prospect = prospectRepository.findByIdAndWorkspaceId(prospectId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect not found"));

        return emailEventRepository.findAllByProspectIdOrderByCreatedAtDesc(prospect.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmailEventResponse> getWorkspaceEmailEvents(String ownerEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return emailEventRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .map(this::map)
                .toList();
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private EmailEventResponse map(EmailEvent event) {
        return EmailEventResponse.builder()
                .id(event.getId())
                .senderId(event.getSenderAccount().getId())
                .prospectId(event.getProspect().getId())
                .toEmail(event.getToEmail())
                .subject(event.getSubject())
                .status(event.getStatus().name())
                .providerMessageId(event.getProviderMessageId())
                .errorMessage(event.getErrorMessage())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildSuppressedMessage(SuppressionReason reason) {
        return "Recipient is suppressed: " + reason.name();
    }
}
