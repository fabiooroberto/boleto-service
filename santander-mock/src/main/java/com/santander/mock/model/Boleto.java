package com.santander.mock.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "boletos_mock")
public record Boleto(
    @Id String id,
    String environment,
    String nsuCode,
    String nsuDate,
    String covenantCode,
    String bankNumber,
    String clientNumber,
    String dueDate,
    String issueDate,
    String participantCode,
    String nominalValue,
    Payer payer,
    Beneficiary beneficiary,
    String documentKind,
    Discount discount,
    String finePercentage,
    String fineQuantityDays,
    String interestPercentage,
    String deductionValue,
    String protestType,
    String protestQuantityDays,
    String writeOffQuantityDays,
    String paymentType,
    String parcelsQuantity,
    String valueType,
    String minValueOrPercentage,
    String maxValueOrPercentage,
    String iofPercentage,
    List<Sharing> sharing,
    DictKey key,
    String txId,
    List<String> messages,
    String barcode,
    String digitableLine,
    String entryDate,
    String qrCodePix,
    String qrCodeUrl,
    String status,
    String statusComplement,
    Payment payment,
    String createdAt,
    String updatedAt
) {}
