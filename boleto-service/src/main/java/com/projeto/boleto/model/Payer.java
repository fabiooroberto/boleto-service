package com.projeto.boleto.model;

public record Payer(
    String name,
    String documentType,
    String documentNumber,
    String address,
    String neighborhood,
    String city,
    String state,
    String zipCode
) {}
