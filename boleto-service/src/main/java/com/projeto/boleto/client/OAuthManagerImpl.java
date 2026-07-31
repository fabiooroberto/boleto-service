package com.projeto.boleto.client;

import com.projeto.boleto.model.OAuthTokenResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service implementation for managing OAuth token operations.
 * Provides centralized OAuth token management with caching and automatic refresh.
 * Uses thread-safe mechanisms to handle concurrent access.
 *
 * @author Boleto Service Team
 * @version 1.0
 */
@Service
public class OAuthManagerImpl implements OAuthManager {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private String cachedToken;
    private LocalDateTime tokenExpiry;

    /**
     * Constructs a new OAuthManagerImpl with injected dependencies.
     *
     * @param restTemplate the unauthenticated REST template for token requests (must not have OAuth interceptor)
     * @param baseUrl the base URL for Santander API
     * @param clientId the OAuth client ID
     * @param clientSecret the OAuth client secret
     */
    public OAuthManagerImpl(
            @Qualifier("unauthenticatedRestTemplate") RestTemplate restTemplate,
            @Value("${santander.api.base-url}") String baseUrl,
            @Value("${santander.api.client-id}") String clientId,
            @Value("${santander.api.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cachedToken = null;
        this.tokenExpiry = null;
    }

    /**
     * Retrieves a valid OAuth token for API authentication.
     * Returns cached token if still valid (expires in > 60 seconds).
     * Otherwise, refreshes the token before returning.
     *
     * @return the OAuth access token string
     */
    @Override
    public String getToken() {
        if (isTokenValid()) {
            lock.readLock().lock();
            try {
                return cachedToken;
            } finally {
                lock.readLock().unlock();
            }
        }

        refreshToken();

        lock.readLock().lock();
        try {
            return cachedToken;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if the cached token is still valid.
     * A token is valid if it exists and expires in more than 60 seconds.
     *
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid() {
        lock.readLock().lock();
        try {
            if (cachedToken == null || tokenExpiry == null) {
                return false;
            }
            return LocalDateTime.now().plusSeconds(60).isBefore(tokenExpiry);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Refreshes the OAuth token by making a POST request to the token endpoint.
     * Updates the cached token and expiry time.
     */
    public void refreshToken() {
        lock.writeLock().lock();
        try {
            String tokenUrl = baseUrl + "/oauth/token";
            String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            OAuthTokenResponse response = restTemplate.postForObject(
                    tokenUrl,
                    request,
                    OAuthTokenResponse.class);

            if (response != null) {
                this.cachedToken = response.accessToken();
                this.tokenExpiry = LocalDateTime.now().plusSeconds(response.expiresIn());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
