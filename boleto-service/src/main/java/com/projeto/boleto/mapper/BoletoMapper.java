package com.projeto.boleto.mapper;

import com.projeto.boleto.model.Boleto;
import com.projeto.boleto.model.BoletoRequest;
import com.projeto.boleto.model.BoletoResponse;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class BoletoMapper {

    public BoletoRequest withEnvironment(BoletoRequest request, String environment) {
        return new BoletoRequest(
            request.covenantCode(),
            request.bankNumber(),
            request.dueDate(),
            request.nominalValue(),
            request.documentKind(),
            request.payer(),
            request.beneficiary(),
            environment,
            request.nsuCode(),
            request.nsuDate(),
            request.clientNumber(),
            request.issueDate(),
            request.participantCode(),
            request.discount(),
            request.finePercentage(),
            request.fineQuantityDays(),
            request.interestPercentage(),
            request.deductionValue(),
            request.protestType(),
            request.protestQuantityDays(),
            request.writeOffQuantityDays(),
            request.paymentType(),
            request.parcelsQuantity(),
            request.valueType(),
            request.minValueOrPercentage(),
            request.maxValueOrPercentage(),
            request.iofPercentage(),
            request.sharing(),
            request.key(),
            request.txId(),
            request.messages()
        );
    }

    public Boleto toEntity(BoletoResponse response) {
        return new Boleto(
            response.id(),
            response.covenantCode(),
            response.bankNumber(),
            response.dueDate(),
            response.nominalValue(),
            response.documentKind(),
            response.payer(),
            response.beneficiary(),
            response.environment(),
            response.nsuCode(),
            response.nsuDate(),
            response.clientNumber(),
            response.issueDate(),
            response.participantCode(),
            response.discount(),
            response.finePercentage(),
            response.fineQuantityDays(),
            response.interestPercentage(),
            response.deductionValue(),
            response.protestType(),
            response.protestQuantityDays(),
            response.writeOffQuantityDays(),
            response.paymentType(),
            response.parcelsQuantity(),
            response.valueType(),
            response.minValueOrPercentage(),
            response.maxValueOrPercentage(),
            response.iofPercentage(),
            response.sharing(),
            response.key(),
            response.txId(),
            response.messages(),
            response.barcode(),
            response.digitableLine(),
            response.entryDate(),
            response.qrCodePix(),
            response.qrCodeUrl(),
            response.status(),
            response.statusComplement(),
            response.payment(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
