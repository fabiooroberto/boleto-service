package com.projeto.boleto.client;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class OAuthClientHttpInterceptor implements ClientHttpRequestInterceptor {

    private final OAuthManager oauthManager;

    public OAuthClientHttpInterceptor(OAuthManager oauthManager) {
        this.oauthManager = oauthManager;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String token = oauthManager.getToken();
        request.getHeaders().set("Authorization", "Bearer " + token);
        request.getHeaders().set("Content-Type", "application/json");
        return execution.execute(request, body);
    }
}
