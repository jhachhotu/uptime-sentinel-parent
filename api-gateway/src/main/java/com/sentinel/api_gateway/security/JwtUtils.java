package com.sentinel.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${JWT_SECRET}")
    private String secret;

    public String generateToken(Authentication authentication) {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        // 1. Fix: Safely extract only serializable attributes
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getAttribute("email"));
        claims.put("name", user.getAttribute("name"));
        claims.put("picture", user.getAttribute("picture"));
        claims.put("sub", user.getAttribute("sub"));

        // 2. Fix: Use Keys.hmacShaKeyFor with a specific byte array
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getAttribute("email"))
                .setIssuedAt(new Date())
                // Expiration: 24 Hours
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}