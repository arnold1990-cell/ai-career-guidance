package com.edurite.security.service;

import com.edurite.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named JwtService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration:3600}") long accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration:604800}") long refreshTokenExpiration
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Beginner note: this method handles the "generateAccessToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), accessTokenExpiration, Map.of());
    }

    /**
     * Beginner note: this method handles the "generateRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), refreshTokenExpiration, Map.of("type", "refresh"));
    }

    /**
     * Beginner note: this method handles the "generateAccessToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateAccessToken(User user) {
        return generateToken(user.getEmail(), accessTokenExpiration, Map.of());
    }

    /**
     * Beginner note: this method handles the "generateRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateRefreshToken(User user) {
        return generateToken(user.getEmail(), refreshTokenExpiration, Map.of("type", "refresh"));
    }

    /**
     * Beginner note: this method handles the "accessTokenExpirationSeconds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public long accessTokenExpirationSeconds() {
        return accessTokenExpiration;
    }

    /**
     * Beginner note: this method handles the "extractUsername" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Beginner note: this method handles the "isTokenValid" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Beginner note: this method handles the "isRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isRefreshToken(String token) {
        Object type = extractAllClaims(token).get("type");
        return "refresh".equals(type);
    }

    /**
     * Beginner note: this method handles the "generateToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String generateToken(String subject, long expirationSeconds, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Beginner note: this method handles the "extractClaim" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    /**
     * Beginner note: this method handles the "isTokenExpired" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Beginner note: this method handles the "extractAllClaims" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
