package com.santander.mock.controller;

import com.santander.mock.model.OAuthTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OAuthController.
 * Tests OAuth token endpoint with valid and invalid credentials.
 */
@DisplayName("OAuthController Unit Tests")
class OAuthControllerTest {

    private OAuthController oauthController;

    @BeforeEach
    void setUp() {
        oauthController = new OAuthController();
    }

    @Test
    @DisplayName("Should return token with valid credentials")
    void testGetTokenWithValidCredentials() {
        // Act
        OAuthTokenResponse response = oauthController.getToken(
            "client_credentials",
            "test-client",
            "test-secret"
        );

        // Assert
        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
        assertTrue(response.accessToken().contains("."));
    }

    @Test
    @DisplayName("Should return valid JWT token format")
    void testGetTokenReturnsValidJwtFormat() {
        // Act
        OAuthTokenResponse response = oauthController.getToken(
            "client_credentials",
            "test-client",
            "test-secret"
        );

        // Assert - JWT should have 3 parts (header.payload.signature)
        String token = response.accessToken();
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "JWT should have 3 parts separated by dots");
    }

    @Test
    @DisplayName("Should throw exception with invalid client ID")
    void testGetTokenWithInvalidClientId() {
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> oauthController.getToken("client_credentials", "invalid-client", "test-secret")
        );
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception with invalid client secret")
    void testGetTokenWithInvalidClientSecret() {
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> oauthController.getToken("client_credentials", "test-client", "wrong-secret")
        );
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception with invalid grant type")
    void testGetTokenWithInvalidGrantType() {
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> oauthController.getToken("authorization_code", "test-client", "test-secret")
        );
        assertEquals("Invalid grant type: authorization_code", exception.getMessage());
    }

    @Test
    @DisplayName("Should return Bearer token type")
    void testGetTokenReturnsCorrectTokenType() {
        // Act
        OAuthTokenResponse response = oauthController.getToken(
            "client_credentials",
            "test-client",
            "test-secret"
        );

        // Assert
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    @DisplayName("Should return correct expiration time (3600 seconds)")
    void testGetTokenReturnsCorrectExpirationTime() {
        // Act
        OAuthTokenResponse response = oauthController.getToken(
            "client_credentials",
            "test-client",
            "test-secret"
        );

        // Assert
        assertEquals(3600, response.expiresIn());
    }

    @Test
    @DisplayName("Should generate tokens with matching credentials on multiple requests")
    void testGetTokenConsistentResponseFormat() {
        // Act - Get first token
        OAuthTokenResponse response1 = oauthController.getToken(
            "client_credentials",
            "test-client",
            "test-secret"
        );

        // Act - Get second token
        OAuthTokenResponse response2 = oauthController.getToken(
            "client_credentials",
            "test-client",
            "test-secret"
        );

        // Assert - Both responses should have the same structure and values
        assertEquals("Bearer", response1.tokenType());
        assertEquals("Bearer", response2.tokenType());
        assertEquals(3600, response1.expiresIn());
        assertEquals(3600, response2.expiresIn());
        assertNotNull(response1.accessToken());
        assertNotNull(response2.accessToken());
    }
}
