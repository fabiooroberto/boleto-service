package com.projeto.boleto.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ConfigurationLogger {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationLogger.class);
    private final Environment environment;

    public ConfigurationLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        log.info("=== Application Configuration ===");
        log.info("MongoDB URI: {}", environment.getProperty("spring.data.mongodb.uri"));
        log.info("Santander API Base URL: {}", environment.getProperty("santander.api.base-url"));
        log.info("==================================");
    }
}
