package com.projeto.boleto.client;

/**
 * Interface for managing OAuth token operations.
 * Handles obtaining and caching OAuth tokens for API authentication.
 *
 * @author Boleto Service Team
 * @version 1.0
 */
public interface OAuthManager {

    /**
     * Retrieves a valid OAuth token for API authentication.
     * Implementations should handle token caching and refresh as needed.
     *
     * @return the OAuth access token string
     * @throws RuntimeException if token retrieval fails
     */
    String getToken();
}
