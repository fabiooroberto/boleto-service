package com.projeto.boleto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BoletoRequest(
    @NotBlank(message = "covenantCode is required")
    String covenantCode,

    @NotBlank(message = "bankNumber is required")
    String bankNumber,

    @NotBlank(message = "dueDate is required")
    String dueDate,

    @NotBlank(message = "nominalValue is required")
    String nominalValue,

    @NotBlank(message = "documentKind is required")
    String documentKind,

    @NotNull(message = "payer is required")
    Payer payer,

    @NotNull(message = "beneficiary is required")
    Beneficiary beneficiary,

    String environment,

    @NotBlank(message = "nsuCode is required")
    String nsuCode,

    @NotBlank(message = "nsuDate is required")
    String nsuDate,

    String clientNumber,

    @NotBlank(message = "issueDate is required")
    String issueDate,

    String participantCode,
    Discount discount,
    String finePercentage,
    String fineQuantityDays,
    String interestPercentage,
    String deductionValue,
    String protestType,
    String protestQuantityDays,
    String writeOffQuantityDays,

    @NotBlank(message = "paymentType is required")
    String paymentType,

    String parcelsQuantity,
    String valueType,
    String minValueOrPercentage,
    String maxValueOrPercentage,
    String iofPercentage,
    List<Sharing> sharing,
    DictKey key,
    String txId,
    List<String> messages
) {}
