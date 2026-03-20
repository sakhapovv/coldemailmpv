package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sakhapov.emailwarmup.api.dto.SendProspectEmailRequest;
import ru.sakhapov.emailwarmup.store.entity.EmailEvent;
import ru.sakhapov.emailwarmup.store.entity.EmailEventStatus;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.ProspectStatus;
import ru.sakhapov.emailwarmup.store.entity.SenderAccount;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.EmailEventRepository;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.SenderAccountRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailEventServiceImplTest {

    @Mock
    private EmailEventRepository emailEventRepository;
    @Mock
    private ProspectRepository prospectRepository;
    @Mock
    private SenderAccountRepository senderAccountRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private SenderService senderService;
    @Mock
    private TemplateService templateService;
    @Mock
    private SuppressionService suppressionService;
    @Mock
    private UnsubscribeService unsubscribeService;

    @InjectMocks
    private EmailEventServiceImpl emailEventService;

    @Test
    void sendToProspectShouldCreateSkippedEventWhenRecipientSuppressed() {
        Workspace workspace = Workspace.builder().id(10L).build();
        Prospect prospect = Prospect.builder().id(1L).workspace(workspace).email("blocked@test.com").build();
        SenderAccount sender = SenderAccount.builder().id(5L).workspace(workspace).email("sender@test.com").build();
        SendProspectEmailRequest request = new SendProspectEmailRequest();
        request.setSenderId(5L);
        request.setSubject("Hello {{firstName}}");
        request.setText("Hi there");

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(prospectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(prospect));
        when(senderAccountRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(sender));
        when(templateService.render("Hello {{firstName}}", prospect)).thenReturn("Hello");
        when(templateService.render("Hi there", prospect)).thenReturn("Hi there");
        when(suppressionService.findSuppressionReason("owner@test.com", "blocked@test.com"))
                .thenReturn(Optional.of(SuppressionReason.UNSUBSCRIBED));
        when(emailEventRepository.save(any(EmailEvent.class))).thenAnswer(invocation -> {
            EmailEvent event = invocation.getArgument(0);
            event.setId(42L);
            return event;
        });

        assertThatThrownBy(() -> emailEventService.sendToProspect("owner@test.com", 1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recipient is suppressed: UNSUBSCRIBED (eventId=42)");

        ArgumentCaptor<EmailEvent> eventCaptor = ArgumentCaptor.forClass(EmailEvent.class);
        verify(emailEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(EmailEventStatus.SKIPPED);
        assertThat(eventCaptor.getValue().getErrorMessage()).isEqualTo("Suppressed: UNSUBSCRIBED");
        verify(senderService, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    void sendToProspectShouldSuppressAndMarkProspectBouncedOnHardBounce() {
        Workspace workspace = Workspace.builder().id(10L).build();
        Prospect prospect = Prospect.builder()
                .id(1L)
                .workspace(workspace)
                .email("bounce@test.com")
                .status(ProspectStatus.ACTIVE)
                .build();
        SenderAccount sender = SenderAccount.builder().id(5L).workspace(workspace).email("sender@test.com").build();
        SendProspectEmailRequest request = new SendProspectEmailRequest();
        request.setSenderId(5L);
        request.setSubject("Hello");
        request.setText("Hi there");

        when(workspaceRepository.findFirstByOwnerEmail("owner@test.com")).thenReturn(Optional.of(workspace));
        when(prospectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(prospect));
        when(senderAccountRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(sender));
        when(templateService.render("Hello", prospect)).thenReturn("Hello");
        when(templateService.render("Hi there", prospect)).thenReturn("Hi there");
        when(suppressionService.findSuppressionReason("owner@test.com", "bounce@test.com"))
                .thenReturn(Optional.empty());
        when(unsubscribeService.buildUnsubscribeUrl(prospect)).thenReturn("http://localhost:8080/api/public/unsubscribe?token=x");
        when(senderService.sendEmail(eq("owner@test.com"), eq(5L), eq("bounce@test.com"), eq("Hello"), any()))
                .thenThrow(new IllegalArgumentException("Failed to send email: 550 5.1.1 user unknown"));
        when(emailEventRepository.save(any(EmailEvent.class))).thenAnswer(invocation -> {
            EmailEvent event = invocation.getArgument(0);
            event.setId(43L);
            return event;
        });

        assertThatThrownBy(() -> emailEventService.sendToProspect("owner@test.com", 1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to send email: 550 5.1.1 user unknown (eventId=43)");

        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.BOUNCED);
        verify(suppressionService).suppressEmail(workspace, "bounce@test.com", SuppressionReason.BOUNCED);
    }
}
