package ru.sakhapov.emailwarmup.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "email_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_account_id", nullable = false)
    private SenderAccount senderAccount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @Column(nullable = false, length = 320)
    private String toEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmailEventStatus status;

    @Column()
    private String providerMessageId;

    @Column(length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
