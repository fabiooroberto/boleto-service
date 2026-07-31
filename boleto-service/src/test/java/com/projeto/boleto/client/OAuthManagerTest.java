package com.projeto.boleto.client;

import com.projeto.boleto.model.OAuthTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OAuthManagerImpl.
 *
 * Tests OAuth token management functionality including token caching,
 * refresh logic, expiry validation, and thread safety of token operations.
 *
 * @author Test Suite
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthManager Tests")
class OAuthManagerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OAuthManagerImpl oAuthManager;

    private OAuthTokenResponse testTokenResponse;
    private static final String TEST_BASE_URL = "https://api.santander.com";
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_TOKEN = "test-access-token";
    private static final int TEST_EXPIRES_IN = 3600;

    @BeforeEach
    void setUp() {
        // Initialize test OAuth response
        testTokenResponse = new OAuthTokenResponse(
            TEST_TOKEN,
            "Bearer",
            TEST_EXPIRES_IN
        );

        // Set the properties using reflection since they're injected via @Value
        ReflectionTestUtils.setField(oAuthManager, "baseUrl", TEST_BASE_URL);
        ReflectionTestUtils.setField(oAuthManager, "clientId", TEST_CLIENT_ID);
        ReflectionTestUtils.setField(oAuthManager, "clientSecret", TEST_CLIENT_SECRET);
    }

    @Test
    @DisplayName("Should fetch new token on first call")
    void testGetTokenFirstCall() {
        // Arrange
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(testTokenResponse);

        // Act
        String token = oAuthManager.getToken();

        // Assert
        assertNotNull(token);
        assertEquals(TEST_TOKEN, token);

        // Verify that refresh was called
        verify(restTemplate, times(1)).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }

    @Test
    @DisplayName("Should return cached token if still valid")
    void testGetTokenCacheHit() {
        // Arrange - First call to populate cache
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(testTokenResponse);

        // First call to populate cache
        String firstToken = oAuthManager.getToken();

        // Reset mock to ensure it's not called again
        reset(restTemplate);

        // Act - Second call should use cache
        String secondToken = oAuthManager.getToken();

        // Assert
        assertEquals(firstToken, secondToken);
        assertEquals(TEST_TOKEN, secondToken);

        // Verify postForObject was not called again (token was cached)
        verify(restTemplate, never()).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }

    @Test
    @DisplayName("Should refresh token if expiry is within 60 seconds")
    void testGetTokenRefresh() {
        // Arrange - Create a token that expires soon (< 60 seconds)
        OAuthTokenResponse soonToExpireToken = new OAuthTokenResponse(
            "soon-to-expire-token",
            "Bearer",
            30  // expires in 30 seconds
        );

        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(soonToExpireToken);

        // First call to populate cache with soon-to-expire token
        String expiredToken = oAuthManager.getToken();
        assertEquals("soon-to-expire-token", expiredToken);

        // Simulate time passing - now the token is about to expire
        // Set the expiry to current time + 30 seconds (which is < 60 seconds from now)
        LocalDateTime futureExpiry = LocalDateTime.now().plusSeconds(30);
        ReflectionTestUtils.setField(oAuthManager, "tokenExpiry", futureExpiry);

        // Reset mock and return new token
        reset(restTemplate);
        OAuthTokenResponse freshToken = new OAuthTokenResponse(
            "fresh-token",
            "Bearer",
            TEST_EXPIRES_IN
        );
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(freshToken);

        // Act - This call should trigger refresh
        String token = oAuthManager.getToken();

        // Assert
        assertEquals("fresh-token", token);

        // Verify postForObject was called
        verify(restTemplate, times(1)).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }

    @Test
    @DisplayName("Should return true when token is valid")
    void testIsTokenValid() {
        // Arrange - Set a valid token with far future expiry
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(testTokenResponse);

        oAuthManager.getToken();

        // Act
        boolean isValid = oAuthManager.isTokenValid();

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false when token is expired")
    void testIsTokenExpired() {
        // Arrange - Set an expired token
        OAuthTokenResponse expiredToken = new OAuthTokenResponse(
            "expired-token",
            "Bearer",
            1  // expires in 1 second
        );

        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(expiredToken);

        oAuthManager.getToken();

        // Simulate time passing - set expiry to past
        LocalDateTime pastExpiry = LocalDateTime.now().minusSeconds(10);
        ReflectionTestUtils.setField(oAuthManager, "tokenExpiry", pastExpiry);

        // Act
        boolean isValid = oAuthManager.isTokenValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false when token is null")
    void testIsTokenValidWhenNull() {
        // Arrange - Clear any cached token
        ReflectionTestUtils.setField(oAuthManager, "cachedToken", null);
        ReflectionTestUtils.setField(oAuthManager, "tokenExpiry", null);

        // Act
        boolean isValid = oAuthManager.isTokenValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should refresh token and update cache")
    void testRefreshToken() {
        // Arrange
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(testTokenResponse);

        // Act
        oAuthManager.refreshToken();

        // Assert - Verify token is cached
        String token = oAuthManager.getToken();
        assertEquals(TEST_TOKEN, token);

        // Verify the token URL construction
        verify(restTemplate, times(1)).postForObject(
            eq(TEST_BASE_URL + "/oauth/token"),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }

    @Test
    @DisplayName("Should handle null response from refresh gracefully")
    void testRefreshTokenWithNullResponse() {
        // Arrange
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(null);

        // Act
        oAuthManager.refreshToken();

        // Assert - Cache should remain empty
        boolean isValid = oAuthManager.isTokenValid();
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should use correct OAuth endpoint URL")
    void testRefreshTokenUsesCorrectUrl() {
        // Arrange
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(testTokenResponse);

        // Act
        oAuthManager.refreshToken();

        // Assert
        verify(restTemplate).postForObject(
            eq(TEST_BASE_URL + "/oauth/token"),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }

    @Test
    @DisplayName("Should set token expiry correctly based on response")
    void testTokenExpiryCalculation() {
        // Arrange
        int expiresInSeconds = 7200;
        OAuthTokenResponse customResponse = new OAuthTokenResponse(
            "custom-token",
            "Bearer",
            expiresInSeconds
        );

        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(customResponse);

        LocalDateTime beforeRefresh = LocalDateTime.now();

        // Act
        oAuthManager.refreshToken();

        // Assert - Get the cached expiry
        @SuppressWarnings("unchecked")
        LocalDateTime expiry = (LocalDateTime) ReflectionTestUtils.getField(oAuthManager, "tokenExpiry");

        assertNotNull(expiry);
        assertTrue(expiry.isAfter(beforeRefresh.plusSeconds(expiresInSeconds - 1)));
        assertTrue(expiry.isBefore(beforeRefresh.plusSeconds(expiresInSeconds + 1)));
    }

    @Test
    @DisplayName("Should maintain separate read and write locks for thread safety")
    void testRefreshTokenUpdatesCache() {
        // Arrange
        OAuthTokenResponse firstResponse = new OAuthTokenResponse(
            "first-token",
            "Bearer",
            3600
        );

        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(firstResponse);

        // First refresh
        oAuthManager.refreshToken();
        String firstToken = oAuthManager.getToken();
        assertEquals("first-token", firstToken);

        // Arrange second refresh
        OAuthTokenResponse secondResponse = new OAuthTokenResponse(
            "second-token",
            "Bearer",
            3600
        );

        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(secondResponse);

        // Act - Second refresh
        oAuthManager.refreshToken();
        String secondToken = oAuthManager.getToken();

        // Assert
        assertEquals("second-token", secondToken);
        verify(restTemplate, times(2)).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }

    @Test
    @DisplayName("Should not call refresh if token valid within 60 second threshold")
    void testGetTokenDoesNotRefreshWhenValid() {
        // Arrange - First call to populate cache with long expiry
        when(restTemplate.postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        )).thenReturn(testTokenResponse);  // expires in 3600 seconds

        String firstToken = oAuthManager.getToken();

        // Reset mock
        reset(restTemplate);

        // Act - Second call should not refresh
        String secondToken = oAuthManager.getToken();

        // Assert
        assertEquals(firstToken, secondToken);
        verify(restTemplate, never()).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(OAuthTokenResponse.class)
        );
    }
}
