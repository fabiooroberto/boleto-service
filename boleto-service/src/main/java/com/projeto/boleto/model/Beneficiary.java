package com.projeto.boleto.model;

public record Beneficiary(
    String name,
    String documentType,
    String documentNumber
) {}
