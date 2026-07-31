package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BoletoRequest(
    @NotBlank(message = "environment is required")
    String environment,

    @NotBlank(message = "nsuCode is required")
    @JsonProperty("nsuCode")
    String nsuCode,

    @NotBlank(message = "nsuDate is required")
    @JsonProperty("nsuDate")
    String nsuDate,

    @NotBlank(message = "covenantCode is required")
    @JsonProperty("covenantCode")
    String covenantCode,

    @NotBlank(message = "bankNumber is required")
    @JsonProperty("bankNumber")
    String bankNumber,

    @JsonProperty("clientNumber")
    String clientNumber,

    @NotBlank(message = "dueDate is required")
    @JsonProperty("dueDate")
    String dueDate,

    @NotBlank(message = "issueDate is required")
    @JsonProperty("issueDate")
    String issueDate,

    @JsonProperty("participantCode")
    String participantCode,

    @NotBlank(message = "nominalValue is required")
    @JsonProperty("nominalValue")
    String nominalValue,

    @NotNull(message = "payer is required")
    Payer payer,

    @NotNull(message = "beneficiary is required")
    Beneficiary beneficiary,

    @NotBlank(message = "documentKind is required")
    @JsonProperty("documentKind")
    String documentKind,

    Discount discount,

    @JsonProperty("finePercentage")
    String finePercentage,

    @JsonProperty("fineQuantityDays")
    String fineQuantityDays,

    @JsonProperty("interestPercentage")
    String interestPercentage,

    @JsonProperty("deductionValue")
    String deductionValue,

    @JsonProperty("protestType")
    String protestType,

    @JsonProperty("protestQuantityDays")
    String protestQuantityDays,

    @JsonProperty("writeOffQuantityDays")
    String writeOffQuantityDays,

    @NotBlank(message = "paymentType is required")
    @JsonProperty("paymentType")
    String paymentType,

    @JsonProperty("parcelsQuantity")
    String parcelsQuantity,

    @JsonProperty("valueType")
    String valueType,

    @JsonProperty("minValueOrPercentage")
    String minValueOrPercentage,

    @JsonProperty("maxValueOrPercentage")
    String maxValueOrPercentage,

    @JsonProperty("iofPercentage")
    String iofPercentage,

    List<Sharing> sharing,

    DictKey key,

    String txId,

    List<String> messages,

    String status,

    @JsonProperty("statusComplement")
    String statusComplement,

    Payment payment
) {}
