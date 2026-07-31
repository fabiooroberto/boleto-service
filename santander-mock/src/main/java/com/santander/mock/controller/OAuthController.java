package com.santander.mock.controller;

import com.santander.mock.model.OAuthTokenResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * OAuth Controller for token endpoint.
 * Handles client credentials flow for mock Santander API.
 */
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    private static final String VALID_CLIENT_ID = "test-client";
    private static final String VALID_CLIENT_SECRET = "test-secret";
    private static final String SECRET_KEY = "secret-key-for-jwt-signing-mock-application";
    private static final int TOKEN_EXPIRY_SECONDS = 3600;

    @PostMapping("/token")
    public OAuthTokenResponse getToken(
            @RequestParam(value = "grant_type") String grantType,
            @RequestParam(value = "client_id") String clientId,
            @RequestParam(value = "client_secret") String clientSecret) {

        // Validate grant type
        if (!"client_credentials".equals(grantType)) {
            throw new RuntimeException("Invalid grant type: " + grantType);
        }

        // Validate client credentials
        if (!VALID_CLIENT_ID.equals(clientId) || !VALID_CLIENT_SECRET.equals(clientSecret)) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate JWT token
        String token = generateJwtToken();

        // Return token response
        return new OAuthTokenResponse(token, "Bearer", TOKEN_EXPIRY_SECONDS);
    }

    /**
     * Generates a JWT token valid for 3600 seconds.
     *
     * @return JWT token string
     */
    private String generateJwtToken() {
        long now = System.currentTimeMillis();
        long expiryTime = now + (TOKEN_EXPIRY_SECONDS * 1000L);

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

        return Jwts.builder()
                .setSubject(VALID_CLIENT_ID)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiryTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
