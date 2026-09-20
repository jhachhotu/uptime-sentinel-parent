package com.sentinel.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT utility for the API Gateway.
 * The gateway currently acts as a transparent proxy (all auth is handled by monitoring-service).
 * This class is retained for potential future use (e.g., gateway-level route protection).
 */
@Component
public class JwtUtils {

    private final Key key;

    public JwtUtils(@Value("${JWT_SECRET:defaultSecretKeyForTestingPurposeOnlyButMakeItLongEnoughForHS256Algorithm}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates a JWT token and returns the email (subject) if valid.
     */
    public String validateTokenAndGetEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Check expiration
        if (claims.getExpiration().before(new Date())) {
            throw new RuntimeException("Token expired");
        }

        return claims.getSubject();
    }

    /**
     * Checks if a token is valid (non-expired, correctly signed).
     */
    public boolean isTokenValid(String token) {
        try {
            validateTokenAndGetEmail(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}