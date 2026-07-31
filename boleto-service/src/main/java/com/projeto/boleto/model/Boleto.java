package com.projeto.boleto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "boletos")
public record Boleto(
    @Id String id,
    String covenantCode,
    String bankNumber,
    String dueDate,
    String nominalValue,
    String documentKind,
    Payer payer,
    Beneficiary beneficiary,
    String environment,
    String nsuCode,
    String nsuDate,
    String clientNumber,
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
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
