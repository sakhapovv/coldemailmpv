package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.CreateSenderRequest;
import ru.sakhapov.emailwarmup.api.dto.SenderResponse;
import ru.sakhapov.emailwarmup.api.dto.SenderTestResponse;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;
import ru.sakhapov.emailwarmup.store.entity.SenderStatus;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.SenderAccountRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class SenderService {

    private final SenderAccountRepository senderAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final CryptoService cryptoService;
    @Value("${sender.smtp.test-timeout-ms:10000}")
    private int smtpTestTimeoutMs;

    @Transactional
    public SenderResponse createSender(String ownerEmail, CreateSenderRequest request) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        if (senderAccountRepository.existsByWorkspaceIdAndEmailIgnoreCase(workspace.getId(), request.getEmail())) {
            throw new IllegalArgumentException("Sender with this email already exists in workspace");
        }

        SenderAccount sender = senderAccountRepository.save(
                SenderAccount.builder()
                        .workspace(workspace)
                        .email(request.getEmail())
                        .smtpHost(request.getSmtpHost())
                        .smtpPort(request.getSmtpPort())
                        .smtpUsername(request.getSmtpUsername())
                        .smtpPasswordEncrypted(cryptoService.encrypt(request.getSmtpPassword()))
                        .fromName(request.getFromName())
                        .startTls(request.isStartTls())
                        .ssl(request.isSsl())
                        .status(SenderStatus.ACTIVE)
                        .build()
        );

        return map(sender);
    }

    @Transactional(readOnly = true)
    public List<SenderResponse> listSenders(String ownerEmail) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        return senderAccountRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public SenderTestResponse testSender(String ownerEmail, Long senderId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        SenderAccount sender = senderAccountRepository.findByIdAndWorkspaceId(senderId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        String smtpPassword = cryptoService.decrypt(sender.getSmtpPasswordEncrypted());
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(sender.getSmtpHost());
        mailSender.setPort(sender.getSmtpPort());
        mailSender.setUsername(sender.getSmtpUsername());
        mailSender.setPassword(smtpPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(sender.isStartTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(sender.isSsl()));
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtpTestTimeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(smtpTestTimeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(smtpTestTimeoutMs));

        try {
            mailSender.testConnection();
            return SenderTestResponse.builder()
                    .success(true)
                    .message("SMTP connection successful")
                    .build();
        } catch (Exception ex) {
            throw new IllegalArgumentException("SMTP connection failed: " + ex.getMessage());
        }
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private SenderResponse map(SenderAccount sender) {
        return SenderResponse.builder()
                .id(sender.getId())
                .email(sender.getEmail())
                .smtpHost(sender.getSmtpHost())
                .smtpPort(sender.getSmtpPort())
                .smtpUsername(sender.getSmtpUsername())
                .fromName(sender.getFromName())
                .startTls(sender.isStartTls())
                .ssl(sender.isSsl())
                .status(sender.getStatus().name())
                .createdAt(sender.getCreatedAt())
                .build();
    }
}
