package com.projeto.boleto.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthClientHttpInterceptorTest {

    @Mock
    private OAuthManager oauthManager;

    @Mock
    private ClientHttpRequestExecution execution;

    private OAuthClientHttpInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new OAuthClientHttpInterceptor(oauthManager);
    }

    @Test
    void shouldAddAuthorizationHeaderToRequest() throws Exception {
        // Arrange
        String testToken = "test-access-token-12345";
        when(oauthManager.getToken()).thenReturn(testToken);

        MockClientHttpRequest request = new MockClientHttpRequest();
        byte[] body = new byte[0];

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(mockResponse);

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        assertEquals("Bearer " + testToken, request.getHeaders().getFirst("Authorization"));
        assertEquals("application/json", request.getHeaders().getFirst("Content-Type"));
    }

    @Test
    void shouldCallExecutionWithModifiedRequest() throws Exception {
        // Arrange
        when(oauthManager.getToken()).thenReturn("test-token");

        MockClientHttpRequest request = new MockClientHttpRequest();
        byte[] body = "test body".getBytes();

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(mockResponse);

        // Act
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Assert
        assertEquals(mockResponse, result);
    }
}
