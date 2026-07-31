package com.projeto.boleto.model;

public record BankSlipListQuery(
    int limit,
    int offset,
    String bankNumber,
    String clientNumber,
    String dueDateInitial,
    String dueDateFinal,
    String paymentDateInitial,
    String paymentDateFinal,
    String status
) {}
