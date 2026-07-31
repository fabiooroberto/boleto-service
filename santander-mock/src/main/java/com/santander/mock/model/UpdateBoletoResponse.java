package com.santander.mock.model;

public record UpdateBoletoResponse(
    String covenantCode,
    String bankNumber,
    String message
) {}
