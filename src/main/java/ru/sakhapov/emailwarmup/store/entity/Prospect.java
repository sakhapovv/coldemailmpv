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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "prospects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "email"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 255)
    private String firstName;

    @Column(length = 255)
    private String lastName;

    @Column(length = 255)
    private String company;

    @Column(length = 255)
    private String jobTitle;

    @Column(length = 512)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProspectStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
