package com.projeto.boleto.client;

public class SantanderApiException extends RuntimeException {

    private final int status;
    private final String message;
    private final String error;

    public SantanderApiException(int status, String message, String error) {
        super(error);
        this.status = status;
        this.message = message;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return message;
    }

    public String getError() {
        return error;
    }
}
