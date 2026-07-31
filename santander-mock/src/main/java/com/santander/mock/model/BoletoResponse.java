package com.santander.mock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BoletoResponse(
    String id,
    String environment,
    @JsonProperty("nsuCode")
    String nsuCode,
    @JsonProperty("nsuDate")
    String nsuDate,
    @JsonProperty("covenantCode")
    String covenantCode,
    @JsonProperty("bankNumber")
    String bankNumber,
    @JsonProperty("clientNumber")
    String clientNumber,
    @JsonProperty("dueDate")
    String dueDate,
    @JsonProperty("issueDate")
    String issueDate,
    @JsonProperty("participantCode")
    String participantCode,
    @JsonProperty("nominalValue")
    String nominalValue,
    Payer payer,
    Beneficiary beneficiary,
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
    // Response-only fields
    String barcode,
    @JsonProperty("digitableLine")
    String digitableLine,
    @JsonProperty("entryDate")
    String entryDate,
    @JsonProperty("qrCodePix")
    String qrCodePix,
    @JsonProperty("qrCodeUrl")
    String qrCodeUrl,
    String status,
    String statusComplement,
    Payment payment
) {}
