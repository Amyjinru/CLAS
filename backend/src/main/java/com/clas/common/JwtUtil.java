package com.clas.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;
    private static final int MIN_SECRET_LENGTH = 32;
    private static final Set<String> FORBIDDEN_DEFAULT_SECRETS = Set.of(
        "clas-dev-secret-key-2026",
        "clas-dev-secret-key-2026-must-be-32bytes!"
    );

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration-ms:86400000}") long expirationMs,
                   Environment environment) {
        String normalizedSecret = validateSecret(secret, environment);
        this.key = Keys.hmacShaKeyFor(normalizedSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    private String validateSecret(String secret, Environment environment) {
        String normalizedSecret = secret == null ? "" : secret.trim();
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (!testProfile) {
            if (normalizedSecret.isEmpty()) {
                throw new IllegalStateException("JWT_SECRET must be configured outside the test profile");
            }
            if (FORBIDDEN_DEFAULT_SECRETS.contains(normalizedSecret)) {
                throw new IllegalStateException("JWT_SECRET must not use a development default outside the test profile");
            }
            if (normalizedSecret.length() < MIN_SECRET_LENGTH) {
                throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
            }
        }
        if (normalizedSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least " + MIN_SECRET_LENGTH + " characters for HS256");
        }
        return normalizedSecret;
    }

    public String generateToken(String phone, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(phone)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getPhoneFromToken(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            return parseToken(token).get("role", String.class);
        } catch (JwtException e) {
            return null;
        }
    }
}
