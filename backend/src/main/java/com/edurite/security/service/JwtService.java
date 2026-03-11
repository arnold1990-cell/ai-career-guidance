package com.edurite.security.service; // declares the package path for this Java file

import com.edurite.user.entity.User; // imports a class so it can be used in this file
import io.jsonwebtoken.Claims; // imports a class so it can be used in this file
import io.jsonwebtoken.Jwts; // imports a class so it can be used in this file
import io.jsonwebtoken.security.Keys; // imports a class so it can be used in this file
import java.nio.charset.StandardCharsets; // imports a class so it can be used in this file
import java.time.Instant; // imports a class so it can be used in this file
import java.util.Date; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import java.util.function.Function; // imports a class so it can be used in this file
import javax.crypto.SecretKey; // imports a class so it can be used in this file
import org.springframework.beans.factory.annotation.Value; // imports a class so it can be used in this file
import org.springframework.security.core.userdetails.UserDetails; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named JwtService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class JwtService { // defines a class type

    private final SecretKey signingKey; // executes this statement as part of the application logic
    private final long accessTokenExpiration; // handles authentication or authorization to protect secure access
    private final long refreshTokenExpiration; // handles authentication or authorization to protect secure access

    public JwtService( // handles authentication or authorization to protect secure access
            @Value("${security.jwt.secret}") String secret, // injects a value from configuration properties
            @Value("${security.jwt.access-token-expiration:3600}") long accessTokenExpiration, // injects a value from configuration properties
            @Value("${security.jwt.refresh-token-expiration:604800}") long refreshTokenExpiration // injects a value from configuration properties
    ) { // supports the surrounding application logic
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // executes this statement as part of the application logic
        this.accessTokenExpiration = accessTokenExpiration; // handles authentication or authorization to protect secure access
        this.refreshTokenExpiration = refreshTokenExpiration; // handles authentication or authorization to protect secure access
    } // ends the current code block

    /**
     * Note: this method handles the "generateAccessToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateAccessToken(UserDetails userDetails) { // handles authentication or authorization to protect secure access
        return generateToken(userDetails.getUsername(), accessTokenExpiration, Map.of()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "generateRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateRefreshToken(UserDetails userDetails) { // handles authentication or authorization to protect secure access
        return generateToken(userDetails.getUsername(), refreshTokenExpiration, Map.of("type", "refresh")); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "generateAccessToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateAccessToken(User user) { // handles authentication or authorization to protect secure access
        return generateToken(user.getEmail(), accessTokenExpiration, Map.of()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "generateRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateRefreshToken(User user) { // handles authentication or authorization to protect secure access
        return generateToken(user.getEmail(), refreshTokenExpiration, Map.of("type", "refresh")); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "accessTokenExpirationSeconds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public long accessTokenExpirationSeconds() { // handles authentication or authorization to protect secure access
        return accessTokenExpiration; // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "extractUsername" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String extractUsername(String token) { // handles authentication or authorization to protect secure access
        return extractClaim(token, Claims::getSubject); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "isTokenValid" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) { // handles authentication or authorization to protect secure access
        String username = extractUsername(token); // handles authentication or authorization to protect secure access
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "isRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isRefreshToken(String token) { // handles authentication or authorization to protect secure access
        Object type = extractAllClaims(token).get("type"); // handles authentication or authorization to protect secure access
        return "refresh".equals(type); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "generateToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String generateToken(String subject, long expirationSeconds, Map<String, Object> claims) { // handles authentication or authorization to protect secure access
        Instant now = Instant.now(); // executes this statement as part of the application logic
        return Jwts.builder() // returns a value from this method to the caller
                .claims(claims) // supports the surrounding application logic
                .subject(subject) // supports the surrounding application logic
                .issuedAt(Date.from(now)) // supports the surrounding application logic
                .expiration(Date.from(now.plusSeconds(expirationSeconds))) // supports the surrounding application logic
                .signWith(signingKey) // supports the surrounding application logic
                .compact(); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "extractClaim" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private <T> T extractClaim(String token, Function<Claims, T> resolver) { // handles authentication or authorization to protect secure access
        Claims claims = extractAllClaims(token); // handles authentication or authorization to protect secure access
        return resolver.apply(claims); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "isTokenExpired" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean isTokenExpired(String token) { // handles authentication or authorization to protect secure access
        return extractClaim(token, Claims::getExpiration).before(new Date()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "extractAllClaims" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private Claims extractAllClaims(String token) { // handles authentication or authorization to protect secure access
        return Jwts.parser() // returns a value from this method to the caller
                .verifyWith(signingKey) // supports the surrounding application logic
                .build() // supports the surrounding application logic
                .parseSignedClaims(token) // handles authentication or authorization to protect secure access
                .getPayload(); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
