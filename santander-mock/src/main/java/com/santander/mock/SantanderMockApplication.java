package com.santander.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for Santander Mock API.
 * This application simulates the Santander Bill Issuance API endpoints for testing purposes.
 * It runs on port 8081 and uses MongoDB for data persistence.
 */
@SpringBootApplication
public class SantanderMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SantanderMockApplication.class, args);
    }

}
