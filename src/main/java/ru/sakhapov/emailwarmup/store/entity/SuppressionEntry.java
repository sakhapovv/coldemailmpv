package ru.sakhapov.emailwarmup.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "SuppressionEntry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuppressionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workspaceId;

    private String email;

    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

}
