package ru.sakhapov.emailwarmup.store.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sakhapov.emailwarmup.api.dto.UnsubscribeResponse;
import ru.sakhapov.emailwarmup.store.entity.Prospect;
import ru.sakhapov.emailwarmup.store.entity.ProspectStatus;
import ru.sakhapov.emailwarmup.store.entity.SuppressionEntry;
import ru.sakhapov.emailwarmup.store.entity.SuppressionReason;
import ru.sakhapov.emailwarmup.store.entity.Workspace;
import ru.sakhapov.emailwarmup.store.repository.ProspectRepository;
import ru.sakhapov.emailwarmup.store.repository.SuppressionEntryRepository;
import ru.sakhapov.emailwarmup.store.repository.WorkspaceRepository;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class UnsubscribeServiceImpl implements UnsubscribeService {

    private final ProspectRepository prospectRepository;
    private final SuppressionEntryRepository suppressionEntryRepository;
    private final WorkspaceRepository workspaceRepository;
    private final String publicBaseUrl;
    private final String secret;
    private final long expirationMs;

    public UnsubscribeServiceImpl(ProspectRepository prospectRepository,
                                  SuppressionEntryRepository suppressionEntryRepository,
                                  WorkspaceRepository workspaceRepository,
                                  @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl,
                                  @Value("${unsubscribe.token.secret:${security.jwt.secret}}") String secret,
                                  @Value("${unsubscribe.token.expiration-ms:2592000000}") long expirationMs) {
        this.prospectRepository = prospectRepository;
        this.suppressionEntryRepository = suppressionEntryRepository;
        this.workspaceRepository = workspaceRepository;
        this.publicBaseUrl = publicBaseUrl;
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String buildUnsubscribeUrl(Prospect prospect) {
        String token = generateToken(prospect);
        return publicBaseUrl + "/api/public/unsubscribe?token=" + token;
    }

    @Transactional
    public UnsubscribeResponse unsubscribe(String token) {
        Claims claims = parseClaims(token);
        Long workspaceId = claims.get("workspaceId", Long.class);
        Long prospectId = claims.get("prospectId", Long.class);
        String email = claims.getSubject();

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        suppressionEntryRepository.findByWorkspaceIdAndEmailIgnoreCase(workspaceId, email)
                .orElseGet(() -> suppressionEntryRepository.save(
                        SuppressionEntry.builder()
                                .workspace(workspace)
                                .email(email)
                                .reason(SuppressionReason.UNSUBSCRIBED)
                                .build()
                ));

        prospectRepository.findByIdAndWorkspaceId(prospectId, workspaceId)
                .ifPresent(prospect -> prospect.setStatus(ProspectStatus.UNSUBSCRIBED));

        return UnsubscribeResponse.builder()
                .success(true)
                .message("Email unsubscribed successfully")
                .email(email)
                .build();
    }

    private String generateToken(Prospect prospect) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(prospect.getEmail())
                .claims(Map.of(
                        "workspaceId", prospect.getWorkspace().getId(),
                        "prospectId", prospect.getId(),
                        "type", "unsubscribe"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"unsubscribe".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Invalid unsubscribe token");
        }
        return claims;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
