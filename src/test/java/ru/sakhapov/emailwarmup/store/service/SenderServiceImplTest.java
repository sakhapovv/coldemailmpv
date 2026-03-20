package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.CreateSenderRequest;
import ru.sakhapov.emailwarmup.api.dto.SenderResponse;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.SenderAccountRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderServiceImplTest {

    @Mock
    private SenderAccountRepository senderAccountRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private SenderServiceImpl senderService;

    @Test
    void createSenderShouldPersistEncryptedSenderConfig() {
        Workspace workspace = Workspace.builder().id(10L).build();
        CreateSenderRequest request = new CreateSenderRequest();
        request.setEmail("mail@test.com");
        request.setSmtpHost("smtp.gmail.com");
        request.setSmtpPort(465);
        request.setSmtpUsername("mail@test.com");
        request.setSmtpPassword("app-password");
        request.setFromName("Sender");
        request.setStartTls(false);
        request.setSsl(true);
        request.setImapHost("imap.gmail.com");
        request.setImapPort(993);
        request.setImapUsername("mail@test.com");
        request.setImapPassword("imap-password");
        request.setImapSsl(true);

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(senderAccountRepository.existsByWorkspaceIdAndEmailIgnoreCase(10L, "mail@test.com")).thenReturn(false);
        when(cryptoService.encrypt("app-password")).thenReturn("encrypted-password");
        when(cryptoService.encrypt("imap-password")).thenReturn("encrypted-imap-password");
        when(senderAccountRepository.save(any(SenderAccount.class))).thenAnswer(invocation -> {
            SenderAccount sender = invocation.getArgument(0);
            sender.setId(1L);
            return sender;
        });

        SenderResponse response = senderService.createSender("owner@test.com", request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("mail@test.com");
        assertThat(response.getSmtpHost()).isEqualTo("smtp.gmail.com");
        assertThat(response.getSmtpPort()).isEqualTo(465);
        assertThat(response.isSsl()).isTrue();
        assertThat(response.isStartTls()).isFalse();
        assertThat(response.getImapHost()).isEqualTo("imap.gmail.com");
        assertThat(response.getImapPort()).isEqualTo(993);
        assertThat(response.getImapUsername()).isEqualTo("mail@test.com");
        assertThat(response.isImapSsl()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(cryptoService).encrypt("app-password");
        verify(cryptoService).encrypt("imap-password");
    }

    @Test
    void createSenderShouldRejectDuplicateEmailInsideWorkspace() {
        Workspace workspace = Workspace.builder().id(10L).build();
        CreateSenderRequest request = new CreateSenderRequest();
        request.setEmail("mail@test.com");

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(senderAccountRepository.existsByWorkspaceIdAndEmailIgnoreCase(10L, "mail@test.com")).thenReturn(true);

        assertThatThrownBy(() -> senderService.createSender("owner@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sender with this email already exists in workspace");
    }

    @Test
    void listSendersShouldReturnMappedSenders() {
        Workspace workspace = Workspace.builder().id(10L).build();
        SenderAccount sender = SenderAccount.builder()
                .id(1L)
                .workspace(workspace)
                .email("mail@test.com")
                .smtpHost("smtp.gmail.com")
                .smtpPort(465)
                .smtpUsername("mail@test.com")
                .fromName("Sender")
                .startTls(false)
                .ssl(true)
                .imapHost("imap.gmail.com")
                .imapPort(993)
                .imapUsername("mail@test.com")
                .imapSsl(true)
                .status(ru.sakhapov.emailwarmup.store.entity.SenderStatus.ACTIVE)
                .build();

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(senderAccountRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(sender));

        List<SenderResponse> responses = senderService.listSenders("owner@test.com");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getEmail()).isEqualTo("mail@test.com");
        assertThat(responses.getFirst().getImapHost()).isEqualTo("imap.gmail.com");
        assertThat(responses.getFirst().getStatus()).isEqualTo("ACTIVE");
    }
}
