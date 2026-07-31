package com.projeto.boleto.model;

public record Sharing(
    String movementType,
    String chargeType,
    String recipient
) {}
