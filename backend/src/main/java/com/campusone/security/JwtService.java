package com.campusone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
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
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final int MINIMUM_HS256_KEY_BYTES = 32;

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
                .audience()
                .add(properties.getAudience())
                .and()
                .subject(principal.getUserId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(USER_ID_CLAIM, principal.getUserId().toString())
                .claim(EMAIL_CLAIM, principal.getUsername())
                .claim(ROLES_CLAIM, principal.getRoleNames().stream().sorted().toList())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Jws<Claims> parsedToken = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .requireAudience(properties.getAudience())
                    .require(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                    .build()
                    .parseSignedClaims(token);
            if (!Jwts.SIG.HS256.getId().equals(
                    parsedToken.getHeader().getAlgorithm())) {
                throw new JwtException("JWT signing algorithm is invalid.");
            }
            if (!"JWT".equals(parsedToken.getHeader().getType())) {
                throw new JwtException("JWT type is invalid.");
            }
            Claims claims = parsedToken.getPayload();

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
                && claims.roles().equals(principal.getRoleNames())
                && principal.isEnabled();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    private SecretKey createSigningKey(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is required and must contain a standard Base64-encoded key.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedSecret.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET is invalid. Expected standard Base64, not a raw string "
                            + "or Base64URL value. Generate it from at least 32 random bytes.",
                    exception);
        }

        if (keyBytes.length < MINIMUM_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too weak. Its decoded key must contain at least 32 random "
                            + "bytes (256 bits) for HS256.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
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
