package com.projeto.boleto.config;

import com.projeto.boleto.client.SantanderApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SantanderApiException.class)
    public ResponseEntity<Map<String, Object>> handleSantanderApiException(SantanderApiException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", ex.getStatus());
        response.put("message", ex.getErrorMessage());
        response.put("error", ex.getError());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpClientErrorException(HttpClientErrorException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", ex.getStatusCode().value());
        response.put("message", ex.getStatusText());
        response.put("error", ex.getStatusCode().value() + " " + ex.getStatusText());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<Map<String, Object>> handleRestClientException(RestClientException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "Service unavailable");
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", ex.getStatusCode().value());
        response.put("message", ex.getReason() != null ? ex.getReason() : "Request failed");
        response.put("error", ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Validation failed");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        response.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "Internal server error");
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
