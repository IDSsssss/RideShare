package com.example.rideshare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes:480}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.expirationMillis = expirationMinutes * 60 * 1000;
    }

    private static byte[] sha256(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateToken(String subject, String role, Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        var builder = Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp);
        if (userId != null) {
            builder.claim("uid", userId);
        }
        return builder.signWith(signingKey).compact();
    }

    public String parseSubject(String token) {
        return parseAllClaims(token).getSubject();
    }

    public String parseRole(String token) {
        String role = parseAllClaims(token).get("role", String.class);
        return role != null ? role : "ADMIN";
    }

    public Long parseUserId(String token) {
        Number n = parseAllClaims(token).get("uid", Number.class);
        return n == null ? null : n.longValue();
    }

    public boolean isValid(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
