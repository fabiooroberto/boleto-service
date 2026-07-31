package com.projeto.boleto;

import com.projeto.boleto.client.RestSantanderClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BoletoServiceApplicationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertNotNull(restTemplate);
    }

    @Test
    void restTemplateHasOAuthInterceptor() {
        assertTrue(restTemplate.getInterceptors().stream()
            .anyMatch(interceptor -> interceptor.getClass().getSimpleName().equals("OAuthClientHttpInterceptor")),
            "RestTemplate should have OAuthClientHttpInterceptor registered");
    }
}
