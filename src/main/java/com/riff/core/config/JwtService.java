package com.riff.core.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates the application's JWTs.
 *
 * <p>Tokens carry the user id as the {@code sub} claim and the display name as a custom
 * {@code name} claim. Signed with HMAC-SHA256 using {@code riff.jwt.secret}.
 */
@Component
public class JwtService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final String CLAIM_DISPLAY_NAME = "name";

    private final SecretKey signingKey;

    public JwtService(@Value("${riff.jwt.secret}") String secret) {
        // HMAC-SHA256 requires a key of at least 256 bits (32 bytes).
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String displayName) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TOKEN_TTL.toMillis());

        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_DISPLAY_NAME, displayName)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getPayload().getSubject());
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token);
    }
}
