package com.projeto.boleto.model;

public record Discount(
    String discountType,
    String value,
    String dueDate
) {}
