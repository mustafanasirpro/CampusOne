package com.campusone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = createSigningKey(properties.getSecret());
    }

    public String generateAccessToken(CampusOneUserPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .issuer(properties.getIssuer())
                .subject(principal.getUserId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(USER_ID_CLAIM, principal.getUserId().toString())
                .claim(EMAIL_CLAIM, principal.getUsername())
                .claim(ROLES_CLAIM, principal.getRoleNames().stream().sorted().toList())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID subject = UUID.fromString(claims.getSubject());
            UUID claimedUserId = UUID.fromString(claims.get(USER_ID_CLAIM, String.class));
            if (!subject.equals(claimedUserId)) {
                throw new JwtException("JWT subject and user ID do not match.");
            }

            String email = claims.get(EMAIL_CLAIM, String.class);
            if (email == null || email.isBlank()) {
                throw new JwtException("JWT email claim is missing.");
            }

            Set<String> roles = readRoles(claims.get(ROLES_CLAIM));
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                throw new JwtException("JWT expiration claim is missing.");
            }
            return new AccessTokenClaims(
                    claimedUserId,
                    email,
                    roles,
                    expiration.toInstant());
        } catch (IllegalArgumentException exception) {
            throw new JwtException("JWT claims are invalid.", exception);
        }
    }

    public boolean isAccessTokenValid(
            AccessTokenClaims claims,
            CampusOneUserPrincipal principal) {
        return claims.userId().equals(principal.getUserId())
                && claims.email().equalsIgnoreCase(principal.getUsername())
                && principal.isEnabled();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    private SecretKey createSigningKey(String encodedSecret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT_SECRET must decode to at least 32 bytes for HS256.");
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET must be a valid Base64-encoded value.", exception);
        }
    }

    private Set<String> readRoles(Object claimValue) {
        if (!(claimValue instanceof List<?> values)) {
            throw new JwtException("JWT roles claim is missing.");
        }

        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (!(value instanceof String role) || role.isBlank()) {
                throw new JwtException("JWT roles claim is invalid.");
            }
            roles.add(role);
        }
        if (roles.isEmpty()) {
            throw new JwtException("JWT roles claim is empty.");
        }
        return Set.copyOf(roles);
    }
}
