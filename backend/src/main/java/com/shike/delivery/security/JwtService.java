package com.shike.delivery.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationHours;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-hours:24}") long expirationHours
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String createToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(principal.id().toString())
            .claims(Map.of(
                "name", principal.name(),
                "role", principal.role(),
                "merchantId", principal.merchantId() == null ? 0 : principal.merchantId()
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();
    }

    public AuthPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        Number merchantId = claims.get("merchantId", Number.class);
        return new AuthPrincipal(
            Long.parseLong(claims.getSubject()),
            claims.get("name", String.class),
            claims.get("role", String.class),
            merchantId == null || merchantId.longValue() == 0 ? null : merchantId.longValue()
        );
    }
}
