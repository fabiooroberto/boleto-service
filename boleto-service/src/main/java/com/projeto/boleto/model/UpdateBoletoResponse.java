package com.projeto.boleto.model;

public record UpdateBoletoResponse(
    String covenantCode,
    String bankNumber,
    String message
) {}
