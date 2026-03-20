package ru.sakhapov.emailwarmup.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.CreateSenderRequest;
import ru.sakhapov.emailwarmup.api.dto.SendTestEmailRequest;
import ru.sakhapov.emailwarmup.api.dto.SendTestEmailResponse;
import ru.sakhapov.emailwarmup.api.dto.SenderResponse;
import ru.sakhapov.emailwarmup.api.dto.SenderTestResponse;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;
import ru.sakhapov.emailwarmup.store.entity.SenderStatus;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.SenderAccountRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import jakarta.mail.Session;
import jakarta.mail.Store;

@Service
@RequiredArgsConstructor
public class SenderServiceImpl implements SenderService {

    private final SenderAccountRepository senderAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final CryptoService cryptoService;
    @Value("${sender.smtp.test-timeout-ms:10000}")
    private int smtpTestTimeoutMs;
    @Value("${sender.imap.test-timeout-ms:10000}")
    private int imapTestTimeoutMs;

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
                        .imapHost(request.getImapHost())
                        .imapPort(request.getImapPort())
                        .imapUsername(request.getImapUsername())
                        .imapPasswordEncrypted(encryptOptional(request.getImapPassword()))
                        .imapSsl(request.isImapSsl())
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

    @Transactional(readOnly = true, noRollbackFor = IllegalArgumentException.class)
    public SenderTestResponse testSender(String ownerEmail, Long senderId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        SenderAccount sender = senderAccountRepository.findByIdAndWorkspaceId(senderId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        JavaMailSenderImpl mailSender = buildSmtpMailSender(sender);

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

    @Transactional(readOnly = true, noRollbackFor = IllegalArgumentException.class)
    public SenderTestResponse testImapSender(String ownerEmail, Long senderId) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        SenderAccount sender = senderAccountRepository.findByIdAndWorkspaceId(senderId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        if (sender.getImapHost() == null || sender.getImapHost().isBlank()
                || sender.getImapPort() == null
                || sender.getImapUsername() == null || sender.getImapUsername().isBlank()
                || sender.getImapPasswordEncrypted() == null || sender.getImapPasswordEncrypted().isBlank()) {
            throw new IllegalArgumentException("IMAP settings are not configured for sender");
        }

        Session session = buildImapSession(sender);
        try (Store store = session.getStore(sender.isImapSsl() ? "imaps" : "imap")) {
            store.connect(
                    sender.getImapHost(),
                    sender.getImapPort(),
                    sender.getImapUsername(),
                    cryptoService.decrypt(sender.getImapPasswordEncrypted())
            );
            return SenderTestResponse.builder()
                    .success(true)
                    .message("IMAP connection successful")
                    .build();
        } catch (Exception ex) {
            throw new IllegalArgumentException("IMAP connection failed: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true, noRollbackFor = IllegalArgumentException.class)
    public SendTestEmailResponse sendTestEmail(String ownerEmail, Long senderId, SendTestEmailRequest request) {
        MailSendResult result = sendEmail(ownerEmail, senderId, request.getTo(), request.getSubject(), request.getText());
        return SendTestEmailResponse.builder()
                .success(true)
                .message("Test email sent successfully")
                .messageId(result.getMessageId())
                .sentAt(result.getSentAt())
                .build();
    }

    @Transactional(readOnly = true, noRollbackFor = IllegalArgumentException.class)
    public MailSendResult sendEmail(String ownerEmail, Long senderId, String to, String subject, String text) {
        Workspace workspace = getOwnedWorkspace(ownerEmail);
        SenderAccount sender = senderAccountRepository.findByIdAndWorkspaceId(senderId, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        JavaMailSenderImpl mailSender = buildSmtpMailSender(sender);
        Instant sentAt = Instant.now();

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(sender.getEmail(), sender.getFromName() == null || sender.getFromName().isBlank()
                    ? sender.getEmail()
                    : sender.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mimeMessage.setSentDate(java.util.Date.from(sentAt));
            mimeMessage.saveChanges();
            mailSender.send(mimeMessage);

            return MailSendResult.builder()
                    .messageId(mimeMessage.getMessageID())
                    .sentAt(sentAt)
                    .build();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to send email: " + ex.getMessage());
        }
    }

    private Workspace getOwnedWorkspace(String ownerEmail) {
        return workspaceRepository.findFirstByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private JavaMailSenderImpl buildSmtpMailSender(SenderAccount sender) {
        String smtpPassword = cryptoService.decrypt(sender.getSmtpPasswordEncrypted());
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(sender.getSmtpHost());
        mailSender.setPort(sender.getSmtpPort());
        mailSender.setUsername(sender.getSmtpUsername());
        mailSender.setPassword(smtpPassword);

        configureSmtpProperties(mailSender.getJavaMailProperties(), sender);
        return mailSender;
    }

    private void configureSmtpProperties(Properties props, SenderAccount sender) {
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(sender.isStartTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(sender.isSsl()));
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtpTestTimeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(smtpTestTimeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(smtpTestTimeoutMs));
    }

    private Session buildImapSession(SenderAccount sender) {
        Properties props = new Properties();
        if (sender.isImapSsl()) {
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.connectiontimeout", String.valueOf(imapTestTimeoutMs));
            props.put("mail.imaps.timeout", String.valueOf(imapTestTimeoutMs));
            props.put("mail.imaps.writetimeout", String.valueOf(imapTestTimeoutMs));
        } else {
            props.put("mail.store.protocol", "imap");
            props.put("mail.imap.connectiontimeout", String.valueOf(imapTestTimeoutMs));
            props.put("mail.imap.timeout", String.valueOf(imapTestTimeoutMs));
            props.put("mail.imap.writetimeout", String.valueOf(imapTestTimeoutMs));
        }
        return Session.getInstance(props);
    }

    private String encryptOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return cryptoService.encrypt(value);
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
                .imapHost(sender.getImapHost())
                .imapPort(sender.getImapPort())
                .imapUsername(sender.getImapUsername())
                .imapSsl(sender.isImapSsl())
                .lastImapSyncAt(sender.getLastImapSyncAt())
                .status(sender.getStatus().name())
                .createdAt(sender.getCreatedAt())
                .build();
    }
}
