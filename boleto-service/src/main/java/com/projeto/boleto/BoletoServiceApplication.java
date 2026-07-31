package com.projeto.boleto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import com.projeto.boleto.client.OAuthClientHttpInterceptor;
import com.projeto.boleto.client.OAuthManager;

/**
 * Boleto Service main Spring Boot application.
 * Orchestrates boleto issuance and persists to MongoDB.
 */
@SpringBootApplication
public class BoletoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoletoServiceApplication.class, args);
    }

    /**
     * Registers RestTemplate as a Spring bean for HTTP operations.
     * Creates and registers OAuthClientHttpInterceptor to automatically add OAuth tokens to all requests.
     * The OAuth manager is lazily loaded to break the circular dependency.
     *
     * @param oauthManager the OAuth manager for token retrieval (lazily loaded)
     * @return a configured RestTemplate instance with OAuth interceptor
     */
    @Bean
    public RestTemplate restTemplate(@Lazy OAuthManager oauthManager) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
            HttpClients.createDefault());
        RestTemplate restTemplate = new RestTemplate(factory);
        OAuthClientHttpInterceptor oauthInterceptor = new OAuthClientHttpInterceptor(oauthManager);
        restTemplate.getInterceptors().add(oauthInterceptor);
        return restTemplate;
    }

    /**
     * Registers an unauthenticated RestTemplate bean for internal OAuth token requests.
     * This RestTemplate must NOT have the OAuth interceptor to avoid circular dependency:
     * - OAuthClientHttpInterceptor calls OAuthManager.getToken()
     * - OAuthManager uses this unauthenticatedRestTemplate to request tokens
     * - Without this separation, refreshToken() would trigger the interceptor again (infinite loop)
     *
     * @return a plain RestTemplate instance without interceptors
     */
    @Bean
    public RestTemplate unauthenticatedRestTemplate() {
        return new RestTemplate();
    }

}
